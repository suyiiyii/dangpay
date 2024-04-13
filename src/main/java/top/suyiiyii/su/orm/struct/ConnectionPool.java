package top.suyiiyii.su.orm.struct;

import java.sql.Connection;

/**
 * 连接池接口
 *
 * @author suyiiyii
 */
public interface ConnectionPool {
    /**
     * 获取一个连接
     *
     * @return 连接
     */
    Connection getConnection();

    /**
     * 归还一个连接
     *
     * @param conn 连接
     */
    void returnConnection(Connection conn);

}
