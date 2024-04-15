package top.suyiiyii.su.orm.core;


import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.orm.annotation.TableRegister;
import top.suyiiyii.su.orm.struct.Table;
import top.suyiiyii.su.orm.utils.SqlExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * 框架的入口
 * 存储表信息的地方
 * 提供register方法用于注册表信息
 *
 * @author suyiiyii
 */

@Slf4j
public class ModelManger {
    /**
     * 线程安全：对象在构造函数中初始化，次后只读，不涉及线程安全问题
     */
    private final List<Table> tables = new ArrayList<>();
    /**
     * 几张映射表，用于快速查找对应的表的相关信息
     */
    // 实体类到表名的映射
    private final Map<Class<?>, String> class2TableName = new HashMap<>();
    // 表名到实体类的映射
    private final Map<String, Class<?>> tableName2Class = new HashMap<>();
    // 实体类到表对象的映射
    private final Map<Class<?>, Table> class2Table = new HashMap<>();
    // 表名到表对象的映射
    private final Map<String, Table> tableName2Table = new HashMap<>();
    private final ConnectionManger connectionManger;

    /**
     * 框架入口，提供包名和创建连接方法
     *
     * @param packageName       包名
     * @param connectionBuilder 创建连接的方法
     */
    public ModelManger(String packageName, Callable<Connection> connectionBuilder) {
        connectionManger = new ConnectionManger(connectionBuilder);
        scan(packageName);
    }

    public ConnectionManger getConnectionManger() {
        return connectionManger;
    }

    /**
     * 获取一个session（数据库会话，对应为唯一的Connection），开始数据库操作
     *
     * @return session
     */
    public Session getSession() {
        return new Session(this);
    }


    /**
     * 注册表信息
     *
     * @param tableName   表名
     * @param entityClass 实体类
     */
    private void register(String tableName, Class<?> entityClass) {
        Table table = new Table(tableName, entityClass);
        class2TableName.put(entityClass, tableName);
        tableName2Class.put(tableName, entityClass);
        class2Table.put(entityClass, table);
        tableName2Table.put(tableName, table);
        tables.add(table);
        SqlExecutor executor = connectionManger.getSqlExecutor();
        try {
            boolean value = executor.createTable(table);
            if (value) {
                log.warn("表 " + tableName + " 不存在，已成功创建");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        executor.close();
    }

    /**
     * 扫描指定包下的所有类，并自动注册
     *
     * @param packageName 包名
     */
    private void scan(String packageName) {
        log.info("开始扫描" + packageName);
        try {
            // 获取当前线程的上下文类加载器
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            // 获取指定路径下的所有资源
            Enumeration<URL> resources = classLoader.getResources(packageName.replace(".", "/"));
            while (resources.hasMoreElements()) {
                // 获取当前资源的URL
                URL resource = resources.nextElement();
                // 将URL转换为文件
                File directory = new File(resource.getFile());
                // 遍历文件夹
                for (File file : directory.listFiles()) {
                    // 检查文件是否是类文件
                    if (file.getName().endsWith(".class")) {
                        // 获取类名
                        String className = file.getName().substring(0, file.getName().length() - 6);
                        // 加载类
                        Class<?> clazz = Class.forName(packageName + "." + className);
                        // 检查类是否被TableRegister注解
                        if (clazz.isAnnotationPresent(TableRegister.class)) {
                            // 获取TableRegister注解
                            TableRegister tableRegister = clazz.getAnnotation(TableRegister.class);
                            // 将类注册到TableInfo中
                            register(tableRegister.value(), clazz);
                            log.info("扫描到 " + clazz.getName() + " 注册到 " + tableRegister.value());
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        log.info("扫描结束");
    }


    public List<Table> getTables() {
        return tables;
    }

    public Map<Class<?>, String> getClass2TableName() {
        return class2TableName;
    }

    public Map<String, Class<?>> getTableName2Class() {
        return tableName2Class;
    }

    public Map<Class<?>, Table> getClass2Table() {
        return class2Table;
    }

    public Map<String, Table> getTableName2Table() {
        return tableName2Table;
    }

}


