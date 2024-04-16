package top.suyiiyii.su.orm.core;

import top.suyiiyii.su.orm.struct.CallBack;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Sql的查询条件的构建器
 * 使用回调函数
 * 支持链式调用
 * 调用结束方法后会执行传入的回调函数并返回结果
 * 建议使用lambda表达式的方式传入回调函数
 *
 * @author suyiiyii
 */
public class Wrapper {
    /**
     * 线程安全：对象仅作为session的工具类，线程安全同session
     */
    // where条件表
    private final List<String> whereStatement = new ArrayList<>();
    // set条件表
    private final List<String> setStatement = new ArrayList<>();
    // 参数列表
    private final List<Object> params = new ArrayList<>();
    private final CallBack callBack;
    private final Class<?> clazz;
    // 分页信息
    private int pageNum = -1;
    private int pageSize = -1;
    // 用于提前判断可不可能有结果
    private boolean noResult = false;

    /**
     * 构造函数，记录了要操作的表和具体的操作
     *
     * @param clazz    要操作的表
     * @param callBack 回调函数
     */
    public Wrapper(Class<?> clazz, CallBack callBack) {
        this.clazz = clazz;
        this.callBack = callBack;
    }

    /**
     * 获取要操作的表
     * 供回调函数使用
     *
     * @return 要操作的表
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 等于条件
     *
     * @param key   字段名
     * @param value 值
     * @return this
     */
    public Wrapper eq(String key, Object value) {
        whereStatement.add('`' + key + '`' + " = ?");
        params.add(value);
        return this;
    }

    /**
     * 不等条件
     *
     * @param key   字段名
     * @param value 值
     * @return this
     */
    public Wrapper neq(String key, Object value) {
        whereStatement.add('`' + key + '`' + " != ?");
        params.add(value);
        return this;
    }

    /**
     * 全字匹配条件
     *
     * @param key   字段名
     * @param value 值
     * @return this
     */
    public Wrapper like(String key, Object value) {
        whereStatement.add('`' + key + '`' + " LIKE ?");
        params.add(value);
        return this;
    }


    /**
     * 模糊匹配条件
     *
     * @param key   字段名
     * @param value 值
     * @return this
     */
    public Wrapper fuzzLike(String key, String value) {
        whereStatement.add('`' + key + '`' + " LIKE ?");
        params.add("%" + value + "%");
        return this;
    }

    /**
     * 分页
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return this
     */
    public Wrapper limit(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        return this;
    }

    /**
     * 设置字段值，用于更新
     *
     * @param key   字段名
     * @param value 值
     * @return this
     */
    public Wrapper set(String key, Object value) {
        setStatement.add('`' + key + '`' + " = ?");
        params.add(value);
        return this;
    }

    /**
     * in运算符，查询数据库中特定字段为一组特定值的数据
     *
     * @param key
     * @param values
     * @return
     */
    public Wrapper in(String key, List<Object> values) {
        if (values.isEmpty()) {
            noResult = true;
            return this;
        }
        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            placeholders.add("?");
        }
        whereStatement.add('`' + key + '`' + " IN (" + String.join(",", placeholders) + ")");
        params.addAll(values);
        return this;
    }


    /**
     * 构建sql语句，将where条件和set条件拼接到基础sql上
     *
     * @param baseSql 基础sql
     * @return 构建好的sql
     */
    public String buildSql(String baseSql) {
        StringBuilder sql = new StringBuilder(baseSql);
        if (!setStatement.isEmpty()) {
            sql.append(" SET ");
            sql.append(String.join(",", setStatement));
        }

        if (!whereStatement.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereStatement));
        }


        if (pageNum != -1 && pageSize != -1) {
            sql.append(" LIMIT ").append((pageNum - 1) * pageSize).append(",").append(pageSize);
        }

        return sql.toString();
    }


    /**
     * 填充参数
     * 按照参数列表的顺序填充添加相应的条件到PreparedStatement
     *
     * @param ps PreparedStatement
     * @return PreparedStatement
     * @throws SQLException SQL异常
     */
    public PreparedStatement fillParams(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }

    /**
     * 无返回值执行
     * 需要回调函数配合使用
     *
     * @throws SQLException SQL异常
     */
    public void execute() {
        try {
            callBack.call(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 列表返回值查询
     * 需要回调函数配合使用
     *
     * @param <T> 返回类型
     * @return 结果列表
     * @throws SQLException SQL异常
     */
    public <T> List<T> all() {
        if (noResult) {
            return new ArrayList<>();
        }
        try {
            return (List<T>) callBack.call(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 单个对象返回值查询
     * 需要回调函数配合使用
     * 将分页信息设置为1,1，获取第一个结果
     *
     * @param <T> 返回类型
     * @return 结果
     * @throws SQLException SQL异常
     */
    public <T> T first() throws NoSuchElementException {
        pageNum = 1;
        pageSize = 1;
        List<T> list = all();
        if (list.isEmpty()) {
            throw new NoSuchElementException("查询结果为空");
        }
        return list.get(0);
    }

}