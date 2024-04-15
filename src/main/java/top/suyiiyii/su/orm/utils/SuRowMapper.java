/**
 * 此类提供了一个通用的行映射器，用于将数据库结果集（ResultSet）中的数据转换为指定类型的实体对象列表。
 * 主要适用于基于JDBC的操作，通过反射机制自动映射列名到实体类属性，列名与属性名的匹配规则为驼峰命名。
 * 全静态方法，暂时不考虑缓存等性能优化
 * <p>
 * 遍历对象的所有成员，如果返回结果中不存在对应的列名，则该成员为null
 * 需要对象类型和结果集类型一一对应，否则抛出异常
 *
 * @author suyiiyii
 * @version 1.0
 * @date 2023.3.20
 */
package top.suyiiyii.su.orm.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static top.suyiiyii.su.UniversalUtils.camlToDown;

/**
 * SuRowMapper 类定义了静态方法，用于处理从 ResultSet 映射到实体类的相关操作。
 *
 * @param <T> 泛型参数，表示需要映射到的目标实体类类型
 * @author suyiiyii
 */
public class SuRowMapper<T> {
    /**
     * 线程安全：全静态方法，无静态变量，不涉及线程安全问题
     */

    /**
     * RowMapper 方法接收一个数据库结果集和目标实体类类型，
     * 从结果集中遍历每一行并将其转化为对应实体类的对象，最后返回一个实体对象列表。
     *
     * @param entityClazz 目标实体类的 Class 对象
     * @param resultSet   数据库查询结果集
     * @return 包含实体对象的列表
     */
    public static <T> List<T> rowMapper(Class<T> entityClazz, ResultSet resultSet) throws SQLException {
        // 实例化列表以存储映射后的实体对象
        List<T> entityList = new ArrayList<>();
        while (resultSet.next()) {
            // 将当前行的数据映射到实体对象中
            try {
                T entity = getEntity(resultSet, entityClazz);
                entityList.add(entity);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException("映射实体类失败");
            }
        }
        return entityList;
    }

    /**
     * getEntity 方法负责从给定的结果集中获取一行数据，并将其转换为目标实体类的一个实例。
     * 它通过比较数据库列名与实体类属性名来实现自动映射。
     *
     * @param resultSet   数据库查询结果集
     * @param entityClass 目标实体类的 Class 对象
     * @return 实体类的新实例
     */
    public static <T> T getEntity(ResultSet resultSet, Class<T> entityClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
        // 创建并初始化一个新的实体类实例
        Constructor<T> constructor = entityClass.getDeclaredConstructor();
        // 设置构造函数可访问以便实例化
        constructor.setAccessible(true);
        T entity = constructor.newInstance();
        // 获取结果集中的所有列名
        List<String> columnNames = getColumnNames(resultSet);
        for (Field field : entityClass.getDeclaredFields()) {
            // 将字段名转为驼峰命名格式并与列名进行匹配
            String targetName = camlToDown(field.getName());
            if (!columnNames.contains(targetName)) {
                // 若不匹配，则跳过该字段
                continue;
            }
            field.setAccessible(true);
            // 根据匹配到的列名从结果集中获取对应的值，并设置到实体类字段上
            // 如果结果集中的值为 null，resultSet.getObject(targetName) 也会返回 null，在对应类型为int等基本类型的情况下，将会抛出异常
            field.set(entity, resultSet.getObject(targetName));
        }
        return entity;
    }

    /**
     * getColumnNames 方法从给定的结果集中提取所有的列名，并存储在一个字符串列表中。
     *
     * @param resultSet 数据库查询结果集
     * @return 包含结果集所有列名的字符串列表
     * @throws SQLException 异常
     */
    public static List<String> getColumnNames(ResultSet resultSet) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            columnNames.add(resultSet.getMetaData().getColumnName(i));
        }
        return columnNames;
    }
}
