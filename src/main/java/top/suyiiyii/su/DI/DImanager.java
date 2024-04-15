package top.suyiiyii.su.DI;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

/**
 * 依赖注入管理器
 * 通过注册几个简单的单例对象，实现简单的构造器注入
 *
 * @author suyiiyii
 */
@Slf4j
public class DImanager {
    private static final Map<Class<?>, Object> beans = new HashMap<>();

    public static <T> void registerBean(Object bean) {
        beans.put(bean.getClass(), bean);
        log.info("收到简单单例对象: {}", bean.getClass().getSimpleName());
    }

    /**
     * 递归扫描指定包下的所有类的所有字段，如果有@Repository注解，取出并且放到beans中
     * 使用流程待完善
     *
     * @param packageName 包名
     */
    public static void scan(String packageName) {
        log.info("DI scan start");
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace(".", "/");
            Enumeration<URL> resources = classLoader.getResources(path);
            Iterable<URL> iterable = Collections.list(resources);
            for (URL url : iterable) {
                File file = new File(url.getFile());
                for (File f : Objects.requireNonNull(file.listFiles())) {
                    String name = f.getName();
                    if (f.isDirectory()) {
                        scan(packageName + "." + name);
                    } else if (name.endsWith(".class")) {
                        String className = packageName + "." + name.substring(0, name.length() - 6);
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Repository.class)) {
                            Repository repository = clazz.getAnnotation(Repository.class);
                            String beanName = repository.value();
                            Object bean = clazz.newInstance();
                            registerBean(bean);
                            log.info("DI register bean: {}", beanName);
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据类型，返回实例
     * 只适用于基本类型或者已经保存过的实例或者无依赖的实例
     *
     * @param clazz 类型
     * @param <T>   泛型
     * @return 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        try {
            // 如果是基本类型，直接返回
            if (clazz.isPrimitive()) {
                return null;
            }
            // 判断有没有保存过这个类的实例
            if (beans.containsKey(clazz)) {
                return (T) beans.get(clazz);
            }
            // 否则，通过反射创建实例
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据类型，自动执行构造器依赖注入，返回实例
     * 在需要的时候进行递归调用，直到所有的依赖都注入完成
     *
     * @param <T> 泛型
     * @return 实例
     */
    public static <T> T getObj(Class<T> clazz) {
        log.info("开始注入对象: {}", clazz.getSimpleName());
        try {
            // 获取构造器需要的参数
            Class<?>[] parameterTypes = clazz.getConstructors()[0].getParameterTypes();
            // 如果没有参数，直接返回实例
            if (parameterTypes.length == 0) {
                return getBean(clazz);
            }
            // 如果保存过这个类的实例，直接返回
            if (beans.containsKey(clazz)) {
                return (T) beans.get(clazz);
            }
            // 递归调用，直到所有的依赖都注入完成
            Object[] objects = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                objects[i] = getObj(parameterTypes[i]);
            }
            // 用获得的参数，执行构造器，返回实例
            T obj = (T) clazz.getConstructors()[0].newInstance(objects);
            log.info("注入对象完成: {}", clazz.getSimpleName());
            return obj;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}