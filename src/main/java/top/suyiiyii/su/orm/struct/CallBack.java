package top.suyiiyii.su.orm.struct;

import top.suyiiyii.su.orm.core.Wrapper;

import java.sql.SQLException;

/**
 * 回调接口
 * 用于 Wrapper执行sql语句
 *
 * @author suyiiyii
 */
public interface CallBack {
    Object call(Wrapper query) throws SQLException;
}
