package top.suyiiyii.su.orm.utils;

import top.suyiiyii.su.orm.core.ConnectionManger;
import top.suyiiyii.su.orm.struct.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * sql执行器
 * 对session负责（session的工具类）
 *
 * @author suyiiyii
 */
public class SqlExecutor {
    /**
     * 线程安全：对象仅作为session的工具类，线程安全同session
     */
    private final Connection conn;
    // 由于只有connection，无法直接归还连接，所以需要connectionManger
    private final ConnectionManger connectionManger;

    public SqlExecutor(Connection conn, ConnectionManger connectionManger) {
        this.conn = conn;
        this.connectionManger = connectionManger;
    }

    public Connection getConn() {
        return conn;
    }

    /**
     * 获取一个预编译的sql语句
     *
     * @param sql sql语句
     * @return 预编译的sql语句
     * @throws SQLException sql语句错误
     */
    public PreparedStatement getPreparedStatement(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    /**
     * 创建不存在的表
     *
     * @param table 表对象
     * @return 是否创建成功
     */
    public boolean createTable(Table table) throws SQLException {
        // 判断表是否存在
        String sql = TableSqlGenerater.getIsTableExistSql(table);
        ResultSet resultSet = query(sql);
        if (resultSet.next()) {
            return false;
        }
        // 不存在则创建
        sql = TableSqlGenerater.getCreateTableSql(table);
        return execute(sql);
    }

    /**
     * 执行语句 sql版
     *
     * @param sql sql语句
     * @return 是否执行成功
     */
    public boolean execute(String sql) throws SQLException {
        return conn.createStatement().execute(sql);
    }

    /**
     * 执行语句 预编译版
     *
     * @param preparedStatement 预编译的sql语句
     * @return 是否执行成功
     */
    public boolean execute(PreparedStatement preparedStatement, boolean isBatch) throws SQLException {
        try {
            if (isBatch) {
                preparedStatement.executeBatch();
            } else {
                preparedStatement.execute();
            }
            return true;
        } finally {
            connectionManger.returnConnection(conn);
        }
    }

    /**
     * 执行语句 预编译版
     *
     * @param preparedStatement 预编译的sql语句
     * @return 是否执行成功
     */
    public boolean execute(PreparedStatement preparedStatement) throws SQLException {
        return execute(preparedStatement, false);
    }

    /**
     * 查询 sql版
     *
     * @param sql sql语句
     * @return 查询结果
     */
    public ResultSet query(String sql) throws SQLException {
        return conn.createStatement().executeQuery(sql);
    }

    /**
     * 查询 预编译版
     *
     * @param preparedStatement 预编译的sql语句
     * @return 查询结果
     */
    public ResultSet query(PreparedStatement preparedStatement) throws SQLException {
        return preparedStatement.executeQuery();
    }

    /**
     * 提交事务
     */
    public void commit() throws SQLException {
        this.conn.commit();
    }


    /**
     * 回滚事务
     *
     * @throws SQLException SQL异常
     */
    public void rollback() throws SQLException {
        this.conn.rollback();
    }

    /**
     * 关闭连接
     * 实际上是归还连接
     */
    public void close() {
        connectionManger.returnConnection(conn);
    }

    public boolean isAutoCommit() {
        try {
            return conn.getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置自动提交
     *
     * @param autoCommit 是否自动提交
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        conn.setAutoCommit(autoCommit);
    }
}