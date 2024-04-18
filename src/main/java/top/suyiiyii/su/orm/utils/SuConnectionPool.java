package top.suyiiyii.su.orm.utils;


import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.orm.struct.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 连接池
 * 用于管理数据库连接
 *
 * @author suyiiyii
 */
@Slf4j
public class SuConnectionPool implements ConnectionPool {
    /**
     * 线程安全：所有的操作都在锁内完成，使用消息通知来唤醒线程，保证线程安全
     */
    // 日志
    // 建立连接的最大数量
    private final int maxSize;
    // 建立连接的最小数量
    private final int minSize;
    // 新建连接的方法
    private final Callable<Connection> newConnection;
    // 空闲连接池
    private final Set<Connection> availableConnections;
    // 已使用连接池
    private final Set<Connection> usedConnections;
    // 每次增加或减少的步进
    private final int STEP = 2;
    // 空闲连接数量修改的阈值
    private final int THRESHOLD = 3;
    // 锁
    Lock lock = new ReentrantLock();
    // 等待连接信号
    private final Condition waitConnection = lock.newCondition();
    // 等待平衡信号
    private final Condition waitBalance = lock.newCondition();
    // 等待守护线程启动完毕信号
    private final Condition waitDaemon = lock.newCondition();
    // 失败计数
    private int failCount = 0;

    public SuConnectionPool(int maxSize, int minSize, Callable<Connection> newConnection) {
        lock.lock();
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.newConnection = newConnection;
        availableConnections = new HashSet<>();
        usedConnections = new HashSet<>();
        // 初始化连接池
        for (int i = 0; i < minSize; i++) {
            try {
                Connection connection = newConnection.call();
                availableConnections.add(connection);
            } catch (Exception e) {
                // 这里由于.call()方法的异常声明，所以必须写Exception
                log.error("Init connection pool error: %s".formatted(e));
            }
        }
        startDaemon();
        lock.unlock();
    }

    /**
     * 从连接池获取一个连接
     *
     * @return Connection
     */
    @Override
    public Connection getConnection() {
        try {
            // 启动时获取锁，并唤醒守护线程，当当前线程开始等待或运行完成时会释放锁，守护线程会获取锁并运行
            lock.lock();
            waitBalance.signalAll();
            while (availableConnections.isEmpty()) {
                try {
                    log.warn("No available connection, waiting");
                    waitConnection.await();
                } catch (InterruptedException e) {
                    // 理论上不会收到中断信号
                    throw new RuntimeException(e);
                }
            }
            Connection connection;
            connection = availableConnections.iterator().next();
            availableConnections.remove(connection);
            usedConnections.add(connection);
            if (!connection.isValid(1)) {
                log.warn("获取到的连接已失效，重新获取");
                usedConnections.remove(connection);
                connection = this.getConnection();
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将连接归还连接池
     *
     * @param conn Connection
     */
    @Override
    public void returnConnection(Connection conn) {
        try {
            lock.lock();
            if (usedConnections.contains(conn)) {
                usedConnections.remove(conn);
                availableConnections.add(conn);
                // 唤醒等待连接的线程，重新平衡连接池
                waitConnection.signalAll();
                waitBalance.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 向空闲池内添加一个连接
     *
     * @return
     */
    private boolean addConnection(int num) {
        try {
            lock.lock();
            if (availableConnections.size() + usedConnections.size() >= maxSize) {
                return false;
            }
            try {
                while (num-- > 0) {
                    Connection connection = newConnection.call();
                    availableConnections.add(connection);
                }
            } catch (Exception e) {
                log.warn("Add connection error: %s".formatted(e));
                failCount++;
                if (failCount > 250) {
                    throw new RuntimeException("Add connection error too many times");
                }
            }
            log.info("Add connection current available: %d, used: %d".formatted(availableConnections.size(), usedConnections.size()));
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从空闲池内移除一个连接
     *
     * @return 是否成功移除
     */
    private boolean removeConnection(int num) {
        try {
            lock.lock();
            if (availableConnections.isEmpty() || availableConnections.size() + usedConnections.size() <= minSize) {
                return false;
            }
            Connection connection;
            while (num-- > 0 && availableConnections.size() > 0) {
                connection = availableConnections.iterator().next();
                availableConnections.remove(connection);
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.warn("Close connection error: %s".formatted(e));
                }
            }
            log.info("Remove connection current available: %d, used: %d".formatted(availableConnections.size(), usedConnections.size()));
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重新平衡连接池
     */
    private void rebanlance() {
        try {
            lock.lock();
            log.info("Rebalancing available: %d, used: %d".formatted(availableConnections.size(), usedConnections.size()));
            // 检查是否需要扩容
            while (availableConnections.size() + usedConnections.size() < maxSize && availableConnections.size() < THRESHOLD) {
                addConnection(STEP);
            }
            // 检查是否需要缩容
            while (availableConnections.size() > THRESHOLD * 2 && availableConnections.size() > minSize) {
                removeConnection(STEP);
            }
        } finally {
            lock.unlock();
        }

    }

    /**
     * 守护线程，需要的时候会被唤醒，负责平衡连接池
     */
    private void daemon() {
        lock.lock();
        waitDaemon.signalAll();
        while (true) {
            try {
                log.info("Daemon paused available: %d, used: %d".formatted(availableConnections.size(), usedConnections.size()));
                waitBalance.await();
            } catch (InterruptedException e) {
                // 理论上不会收到中断信号
                throw new RuntimeException(e);
            }
            log.info("Daemon running available: %d, used: %d".formatted(availableConnections.size(), usedConnections.size()));
            rebanlance();
            waitConnection.signalAll();
        }
    }

    /**
     * 启动守护线程
     */
    private void startDaemon() {
        Thread thread = new Thread(this::daemon);
        thread.setDaemon(true);
        thread.start();
        try {
            lock.lock();
            waitDaemon.await();
        } catch (InterruptedException e) {
            // 理论上不会收到中断信号
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}

