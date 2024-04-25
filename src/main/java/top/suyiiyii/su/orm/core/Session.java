package top.suyiiyii.su.orm.core;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.orm.struct.Table;
import top.suyiiyii.su.orm.utils.RowSqlGenerater;
import top.suyiiyii.su.orm.utils.SqlExecutor;
import top.suyiiyii.su.orm.utils.SuRowMapper;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


/**
 * CRUD操作
 * 封装orm框架的CRUD操作
 * 用于与业务层交互
 *
 * @author suyiiyii
 */

@Slf4j
public class Session {
    /**
     * 线程安全：每个线程应有自己的session对象，不保证线程安全
     */
    // 待插入数据暂存区
    private final Map<Class<?>, List<Object>> insertCache = new HashMap<>();
    // 对象备份
    private final List<Map.Entry<Object, Object>> cache = new ArrayList<>();
    // 归属于的orm对象
    private final ModelManger modelManger;
    private final SqlExecutor sqlExecutor;


    public Session(ModelManger modelManger) {
        this.modelManger = modelManger;
        this.sqlExecutor = modelManger.getConnectionManger().getSqlExecutor();
    }


    /**
     * 查询表的所有信息
     *
     * @param clazz 表对应的类的字节码对象
     * @param <T>   表对应的类
     * @return 包含表的所有信息的列表
     */
    public <T> List<T> selectAll(Class<T> clazz) throws SQLException {
        String tableName = modelManger.getClass2TableName().get(clazz);
        String sql = RowSqlGenerater.selectAll(tableName);
        ResultSet resultSet = sqlExecutor.query(sql);
        return SuRowMapper.rowMapper(clazz, resultSet);
    }


    /**
     * 根据某一个字段查询
     *
     * @param clazz 表对应的类的字节码对象
     * @param key   字段名
     * @param value 字段的值
     * @param <T>   表对应的类
     * @return 包含表的所有信息的列表
     */
    public <T> List<T> selectByKey(Class<T> clazz, String key, Object value) throws SQLException {
        String tableName = modelManger.getClass2TableName().get(clazz);
        String sql = RowSqlGenerater.selectByKey(tableName, key);
        PreparedStatement preparedStatement = sqlExecutor.getPreparedStatement(sql);
        preparedStatement.setObject(1, value);
        ResultSet resultSet = sqlExecutor.query(preparedStatement);
        return SuRowMapper.rowMapper(clazz, resultSet);
    }

    /**
     * 将待插入的对象装入暂存区
     *
     * @param obj 待插入的对象
     * @param <T> 待插入的对象的类型
     */
    public <T> void add(T obj) {
        Class<?> clazz = obj.getClass();
        if (!insertCache.containsKey(clazz)) {
            insertCache.put(clazz, new ArrayList<>());
        }
        // 拷贝一份对象
        T newObj = UniversalUtils.clone(obj);
        insertCache.get(clazz).add(newObj);

    }

    /**
     * 提交事务
     * 提交暂存区的数据
     */
    public void commit() {
        try {

            // 提交插入
            for (Map.Entry<Class<?>, List<Object>> entry : insertCache.entrySet()) {
                try {
                    batchInsert(entry.getValue());
                } catch (Exception e) {
                    throw new RuntimeException("插入失败" + e);
                }
            }
            if (!cache.isEmpty()) {
                checkUpdate();
            }
            // 提交事务
            if (!sqlExecutor.isAutoCommit()) {
                sqlExecutor.commit();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 回滚事务
     */
    public void rollback() throws SQLException {
        sqlExecutor.rollback();
    }


    /**
     * 批量插入在暂存区内的数据
     *
     * @param list 待插入的对象列表
     * @param <T>  待插入的对象的类型
     */
    public <T> void batchInsert(List<T> list) throws SQLException {
        if (list.isEmpty()) {
            return;
        }
        Class<?> clazz = list.get(0).getClass();
        Table table = modelManger.getClass2Table().get(clazz);
        String sql = RowSqlGenerater.getInsertSql(table);
        PreparedStatement preparedStatement = sqlExecutor.getPreparedStatement(sql);
        for (T obj : list) {
            // 因为缺少部分字段，所以需要计数器保证字段序号的连续
            int cnt = 1;
            for (int i = 0; i < table.columns.size(); i++) {
                // 如果是自增字段则跳过
                if (table.columns.get(i).isAutoIncrement) {
                    continue;
                }
                // 获取字段名
                String fieldName = UniversalUtils.downToCaml(table.columns.get(i).name);
                try {
                    // 获取字段的值
                    Field field = obj.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    preparedStatement.setObject(cnt++, field.get(obj));

                } catch (IllegalAccessException | NoSuchFieldException e) {
                    // 理论上不会出现这种情况
                    throw new RuntimeException("插入失败" + e);
                }
            }
            preparedStatement.addBatch();
        }
        sqlExecutor.execute(preparedStatement, true);
        list.clear();
    }

    /**
     * 插入单个对象
     *
     * @param obj 待插入的对象
     * @param <T> 待插入的对象的类型
     * @return 返回插入的数据的ID
     */
    public <T> int insert(T obj, boolean isNeedId) {
        Table table = modelManger.getClass2Table().get(obj.getClass());
        String sql = RowSqlGenerater.getInsertSql(table);
        PreparedStatement preparedStatement;
        try {
            preparedStatement = sqlExecutor.getPreparedStatement(sql);
            // 因为缺少部分字段，所以需要计数器保证字段序号的连续
            int cnt = 1;
            for (int i = 0; i < table.columns.size(); i++) {
                // 如果是自增字段则跳过
                if (table.columns.get(i).isAutoIncrement) {
                    continue;
                }
                // 获取字段名
                String fieldName = UniversalUtils.downToCaml(table.columns.get(i).name);
                // 获取字段的值
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                preparedStatement.setObject(cnt++, field.get(obj));
            }
            sqlExecutor.execute(preparedStatement);
            if (!isNeedId) {
                return -1;
            }
            // 获取刚刚插入的数据的ID
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
//            // 备用方案：执行SELECT LAST_INSERT_ID()查询
//            String selectLastIdSql = "SELECT LAST_INSERT_ID()";
//            try (PreparedStatement lastIdStmt = sqlExecutor.getPreparedStatement(selectLastIdSql);
//                 ResultSet rs = lastIdStmt.executeQuery()) {
//                if (rs.next()) {
//                    return rs.getInt(1);
//                }
//            }
            return -1;
        } catch (NoSuchFieldException | IllegalAccessException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> int insert(T obj) {
        return insert(obj, false);
    }


    /**
     * 更新单个对象
     * 使用主键作为更新条件
     *
     * @param obj 待更新的对象
     * @param <T> 待更新的对象的类型
     */
    public <T> void update(T obj) {
        Table table = modelManger.getClass2Table().get(obj.getClass());
        String sql = RowSqlGenerater.getUpdateSql(table);
        PreparedStatement preparedStatement;
        try {
            preparedStatement = sqlExecutor.getPreparedStatement(sql);
            // 因为缺少部分字段，所以需要计数器保证字段序号的连续
            int cnt = 1;
            for (int i = 0; i < table.columns.size(); i++) {
                // 如果是主键字段则跳过
                if (table.columns.get(i).isPrimaryKey) {
                    continue;
                }
                // 获取字段名
                String fieldName = UniversalUtils.downToCaml(table.columns.get(i).name);
                // 获取字段的值
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                preparedStatement.setObject(cnt++, field.get(obj));
            }
            // 设置主键
            for (int i = 0; i < table.columns.size(); i++) {
                if (table.columns.get(i).isPrimaryKey) {
                    // 获取字段名
                    String fieldName = UniversalUtils.downToCaml(table.columns.get(i).name);
                    // 获取字段的值
                    Field field = obj.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    preparedStatement.setObject(cnt++, field.get(obj));
                    break;
                }
            }
            sqlExecutor.execute(preparedStatement);
        } catch (NoSuchFieldException | IllegalAccessException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询，使用Warpper
     * 支持链式调用添加对象
     * 使用lambda表达式传入回调函数
     *
     * @param clazz 要查询的表
     * @param <T>   要查询的对象的类型
     * @return 查询结果（具体类型取决于链式调用最后执行的方法）
     */
    public <T> Wrapper query(Class<T> clazz) {
        return new Wrapper(clazz, wrapper -> {
            String tableName = modelManger.getClass2TableName().get(wrapper.getClazz());
            String sql = wrapper.buildSql("SELECT * FROM `" + tableName + "` ");
            PreparedStatement ps = sqlExecutor.getPreparedStatement(sql);
            ps = wrapper.fillParams(ps);
            ResultSet rs = sqlExecutor.query(ps);
            List<T> list = (List<T>) SuRowMapper.rowMapper(wrapper.getClazz(), rs);
            for (T obj : list) {
                cache.add(new AbstractMap.SimpleEntry<>(obj, UniversalUtils.clone(obj)));
            }
            return list;
        });
    }

    /**
     * 删除，使用Warpper
     * 支持链式调用添加对象
     * 使用lambda表达式传入回调函数
     *
     * @param clazz 要查询的表
     * @param <T>   要查询的对象的类型
     * @return 查询结果（具体类型取决于链式调用最后执行的方法）
     */

    public <T> Wrapper delete(Class<T> clazz) {
        return new Wrapper(clazz, wrapper -> {
            String tableName = modelManger.getClass2TableName().get(wrapper.getClazz());
            String sql = wrapper.buildSql("DELETE FROM `" + tableName + "` ");
            PreparedStatement ps = sqlExecutor.getPreparedStatement(sql);
            ps = wrapper.fillParams(ps);
            return sqlExecutor.execute(ps);
        });
    }

    /**
     * 更新，使用Warpper
     * 支持链式调用添加对象
     * 使用lambda表达式传入回调函数
     *
     * @param clazz 要查询的表
     * @param <T>   要查询的对象的类型
     * @return 查询结果（具体类型取决于链式调用最后执行的方法）
     */
    public <T> Wrapper update(Class<T> clazz) {
        return new Wrapper(clazz, wrapper -> {
            String tableName = modelManger.getClass2TableName().get(wrapper.getClazz());
            String sql = wrapper.buildSql("UPDATE `" + tableName + "` ");
            PreparedStatement ps = sqlExecutor.getPreparedStatement(sql);
            ps = wrapper.fillParams(ps);
            return sqlExecutor.execute(ps);
        });
    }

    /**
     * 检查已经查询出的对象是否有更改
     * 如果有更改则进行更新
     */
    private void checkUpdate() throws SQLException {
        List<Object> toUpdate = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : cache) {
            Object ori = entry.getValue();
            Object cur = entry.getKey();
            if (!UniversalUtils.equal(ori, cur)) {
                log.info("找到一个更改" + cur);
                toUpdate.add(cur);
            }
        }
        cache.clear();
        if (!toUpdate.isEmpty()) {
            for (Object obj : toUpdate) {
                this.update(obj);
            }
        }
    }

    /**
     * 设置是否自动提交
     *
     * @param autoCommit 是否自动提交
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        sqlExecutor.setAutoCommit(autoCommit);
    }

    /**
     * 关闭连接
     * 最终是归还连接
     */
    public void close() {
        sqlExecutor.close();
    }

    /**
     * 开启事务
     */
    public void beginTransaction() {
        try {
            sqlExecutor.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 提交事务
     */
    public void commitTransaction() {
        try {
            this.commit();
            sqlExecutor.commit();
            sqlExecutor.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 回滚事务
     */
    public void rollbackTransaction() {
        try {
            sqlExecutor.rollback();
            sqlExecutor.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断当前是否在事务中
     */
    public boolean isTransaction() {
        return !sqlExecutor.isAutoCommit();
    }


    /**
     * 销毁
     */

    public void destroy() {
        try {
            this.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}