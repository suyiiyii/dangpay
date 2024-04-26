package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁服务
 * 仅内部调用
 * 专门用于处理分布式锁
 * 提供接口手动上锁和自动上锁
 * 建议全局单例
 */
@Slf4j
public class LockService {
    private final RedissonClient redissonClient;
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public LockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取一个锁
     *
     * @param key 锁的key
     * @return boolean 是否获取成功
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        ReentrantLock localLock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        log.debug("尝试获取本地锁");
        if (localLock.tryLock()) {
            try {
                RLock redisLock = redissonClient.getLock(key);
                log.debug("尝试获取分布式锁");
                if (redisLock.tryLock(waitTime, leaseTime, unit)) {
                    return true;
                }
            } catch (InterruptedException e) {
                log.error("获取分布式锁失败", e);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * 释放一个锁
     *
     * @param key 锁的key
     */
    public void unlock(String key) {
        ReentrantLock localLock = lockMap.get(key);
        if (localLock != null && localLock.isHeldByCurrentThread()) {
            RLock redisLock = redissonClient.getLock(key);
            redisLock.unlock();
            localLock.unlock();
            log.debug("成功释放锁");
        }
        throw new RuntimeException("逻辑错误！！！当前线程未持有锁");
    }

}
