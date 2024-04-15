package top.suyiiyii.su.IOC;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.RBACService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 依赖注入管理器
 * 通过注册几个简单的单例对象，实现简单的构造器注入
 *
 * @author suyiiyii
 */
@Slf4j
public class IOCmanager {
    private static final Map<Class<?>, Class<?>> Interface2Impl = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Object> globalBeans = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> localBeans = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> beanCount = new ConcurrentHashMap<>();

    /**
     * 获取一个全局单例对象
     */
    public static <T> T getGlobalBean(Class<T> clazz) {
        return (T) globalBeans.get(clazz);
    }

    /**
     * 注册一个全局的单例对象
     */
    public static <T> void registerGlobalBean(Object bean) {
        globalBeans.put(bean.getClass(), bean);
        log.info("收到全局单例对象: {}", bean.getClass().getSimpleName());
    }

    /**
     * 注册一个接口的实现类
     */
    public static void registerInterface2Impl(Class<?> interfaceClass, Class<?> implClass) {
        Interface2Impl.put(interfaceClass, implClass);
        log.info("收到接口实现类: {} -> {}", interfaceClass.getSimpleName(), implClass.getSimpleName());
    }

    /**
     * 递归扫描指定包下的所有类，如果有@Repository注解，注册为接口的实现类
     * 使用流程待完善
     *
     * @param packageName 包名
     */
    public static void implScan(String packageName) {
        log.info("DI Impl scan start");
        try {
            // 获取当前线程的类加载器
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace(".", "/"));
            // 遍历所有的资源
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                // 获取文件夹
                String path = url.getPath();
                File file = new File(path);
                // 遍历文件夹下的所有文件
                for (File f : Objects.requireNonNull(file.listFiles())) {
                    String name = f.getName();
                    // 如果是文件夹，递归调用
                    if (f.isDirectory()) {
                        implScan(packageName + "." + name);
                    } else if (name.endsWith(".class")) {
                        // 如果是类文件，获取类名
                        String className = packageName + "." + name.substring(0, name.length() - 6);
                        Class<?> clazz = Class.forName(className);
                        // 如果有@Repository注解，注册为接口的实现类
                        if (clazz.isAnnotationPresent(Repository.class)) {
                            registerInterface2Impl(clazz.getInterfaces()[0], clazz);
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
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
            if (globalBeans.containsKey(clazz)) {
                return (T) globalBeans.get(clazz);
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
     * 优先使用局部单例对象，其次使用全局单例对象
     *
     * @param clazz 需要的实例的类型
     * @param <T>   需要的实例的类型
     * @return 需要的实例
     */
    public <T> T getObj(Class<T> clazz, boolean isNeedAuthorization) {
        log.info("开始注入对象: {}", clazz.getSimpleName());
        try {
            // 如果保存过这个类的局部实例，直接返回
            if (localBeans.containsKey(clazz)) {
                return (T) localBeans.get(clazz);
            }
            // 如果保存过这个类的全局实例，直接返回
            if (globalBeans.containsKey(clazz)) {
                return (T) globalBeans.get(clazz);
            }
            Class<?> clazzInterface = null;
            // 如果是接口，获取实现类
            if (clazz.isInterface()) {
                clazzInterface = clazz;
                clazz = (Class<T>) Interface2Impl.get(clazz);
            }
            if (clazz.isAnnotationPresent(RBACAuthorization.class) || clazzInterface != null && clazzInterface.isAnnotationPresent(RBACAuthorization.class)) {
                if (clazz.isAnnotationPresent(RBACAuthorization.class) && !clazz.getAnnotation(RBACAuthorization.class).isNeedAuthorization()) {
                    isNeedAuthorization = false;
                }
                if (clazzInterface != null && clazzInterface.isAnnotationPresent(RBACAuthorization.class) && !clazzInterface.getAnnotation(RBACAuthorization.class).isNeedAuthorization()) {
                    isNeedAuthorization = false;
                }
            }
            // 默认注入参数最多的构造函数
            Constructor<?>[] constructors = clazz.getConstructors();
            Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));
            // 获取构造器需要的参数
            Class<?>[] parameterTypes = constructors[0].getParameterTypes();
            // 如果没有参数，直接返回实例
            if (parameterTypes.length == 0) {
                return getBean(clazz);
            }
            // 递归调用，直到所有的依赖都注入完成
            Object[] objects = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                objects[i] = getObj(parameterTypes[i], isNeedAuthorization);
            }
            // 用获得的参数，执行构造器，返回实例
            T obj = (T) clazz.getConstructors()[0].newInstance(objects);
            // 如果对象或者接口有@RBACAuthorization注解，检查是否需要权限验证
            if (clazz.isAnnotationPresent(RBACAuthorization.class) || clazzInterface != null && clazzInterface.isAnnotationPresent(RBACAuthorization.class)) {
                // 如果需要权限验证，创建代理对象
                if (isNeedAuthorization) {
                    log.info("创建代理对象: {}", clazz.getSimpleName());
                    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), new AuthorizationInvocationHandler(obj, getObj(UserRoles.class, false), getObj(RBACService.class, false)));
                }
            }
            log.info("注入对象完成: {}", clazz.getSimpleName());
            return obj;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            // 记录这个类的实例数量
            beanCount.put(clazz, beanCount.getOrDefault(clazz, 0) + 1);
        }
    }

    /**
     * 销毁一个对象
     * 递归调用字段的destroy方法，除了单例对象
     */
    public void destroyObj(Object obj, boolean force) {
        // 如果是动态代理对象，获取目标对象
        if (Proxy.isProxyClass(obj.getClass())) {
            try {
                Field field = Proxy.getInvocationHandler(obj).getClass().getDeclaredField("target");
                field.setAccessible(true);
                obj = field.get(Proxy.getInvocationHandler(obj));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // 如果这个类的实例数量小于1，不销毁
        if (beanCount.getOrDefault(obj.getClass(), 0) < 1 && !force) {
            return;
        }
        log.info("开始销毁对象: {}", obj.getClass().getSimpleName());
        try {
            // 循环销毁字段
            for (var field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                // 跳过基本类型
                if (field.getType().isPrimitive()) {
                    continue;
                }
                // 跳过循环引用
                if (field.getType().isAssignableFrom(obj.getClass())) {
                    continue;
                }
                // 跳过单例对象
                if (globalBeans.containsKey(field.getType())) {
                    continue;
                }
                if (localBeans.containsKey(field.getType())) {
                    continue;
                }
                // 递归调用字段的destroy方法
                destroyObj(field.get(obj));
            }
            try {
                // 执行destroy方法
                obj.getClass().getDeclaredMethod("destroy").invoke(obj);
            } catch (NoSuchMethodException e) {
                // 没有destroy方法，跳过
            }
            log.info("销毁对象完成: {}", obj.getClass().getSimpleName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            // 减少这个类的实例数量
            if (!force) {
                beanCount.put(obj.getClass(), beanCount.get(obj.getClass()) - 1);
            }
        }
    }

    public void destroyObj(Object obj) {
        destroyObj(obj, false);
    }

    /**
     * 注册一个局部的单例对象
     */
    public <T> void registerLocalBean(Object bean) {
        localBeans.put(bean.getClass(), bean);
        log.info("收到局部单例对象: {}", bean.getClass().getSimpleName());
    }

    /**
     * 通过类的全限定名创建对象
     *
     * @param fullClassName 类的全限定名
     */
    public <T> T createObj(String fullClassName) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(fullClassName);
        return getObj((Class<T>) clazz, true);
    }

    /**
     * 销毁所有的局部对象
     */
    public void destroy() {
        for (Map.Entry entry : localBeans.entrySet()) {
            destroyObj(entry.getValue(), true);
        }
    }
}