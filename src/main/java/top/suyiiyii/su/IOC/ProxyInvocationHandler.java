package top.suyiiyii.su.IOC;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_403_ForbiddenException;
import top.suyiiyii.su.orm.core.Session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Proxy(isNeedAuthorization = false)
public class ProxyInvocationHandler implements InvocationHandler {
    private final Object target;
    private final UserRoles userRoles;
    private final RBACService rbacService;
    private final Session db;
    /**
     * 在代理对象被创建时，设置整个代理对象是否需要权限校验
     */
    private final boolean isNeedAuthorization;

    public ProxyInvocationHandler(Object target, UserRoles userRoles, RBACService rbacService, Session db, boolean isNeedAuthorization) {
        this.target = target;
        this.userRoles = userRoles;
        this.rbacService = rbacService;
        this.db = db;
        this.isNeedAuthorization = isNeedAuthorization;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 跳过toString方法
        if (method.getName().equals("toString")) {
            return "Proxy for " + target.getClass().getName();
        }
        // 跳过destroy方法
        if (method.getName().equals("destroy")) {
            return null;
        }

        boolean isNeedAuthorization = false;
        boolean isTransaction = false;
        // 检查对象上是否有Proxy注解
        if (target.getClass().isAnnotationPresent(Proxy.class)) {
            Proxy annotation = target.getClass().getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
        }
        // 检查对象的接口上是否有Proxy注解
        if (method.getDeclaringClass().isAnnotationPresent(Proxy.class)) {
            Proxy annotation = method.getDeclaringClass().getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
        }
        // 检查方法上是否有Proxy注解
        if (method.isAnnotationPresent(Proxy.class)) {
            Proxy annotation = method.getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
            isTransaction = annotation.transaction();
        }
        // 如果需要权限校验，则进行权限校验
        if (isNeedAuthorization && this.isNeedAuthorization) {
            checkAuthorization(method, args);
        }

        try {
            // 如果需要事务，则开启事务
            if (isTransaction) {
                try {
                    db.beginTransaction();
                    Object result = method.invoke(target, args);
                    db.commitTransaction();
                    return result;
                } catch (Exception e) {
                    db.rollbackTransaction();
                    throw e;
                }
            } else {
                return method.invoke(target, args);
            }
        } catch (InvocationTargetException e) {
            // invoke方法抛出的是一个包装过的异常，需要通过getTargetException获取原始异常
            throw e.getTargetException();
        }
    }

    public void destroy() {
    }

    private void checkAuthorization(Method method, Object[] args) {
        String permission = method.getDeclaringClass().getSimpleName() + UniversalUtils.capitalizeFirstLetter(method.getName());
        int subRegionId = 0;
        // 检查有没有参数有子区域注解
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(SubRegion.class)) {
                // 如果参数有子区域注解，则获取子区域id，并拼接权限字符串
                SubRegion annotation = parameter.getAnnotation(SubRegion.class);
                String areaPrefix = annotation.areaPrefix();
                subRegionId = (int) args[i];
                permission += "/" + areaPrefix + subRegionId;
                break;
            }
        }
        boolean result = rbacService.checkUserPermission(userRoles, permission);
        if (!result) {
            String message = "权限校验失败，请求用户: " + userRoles.uid + " 用户角色: " + userRoles.roles + " 请求权限: " + permission;
            log.info("权限校验失败，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
//            throw new Http_403_ForbiddenException("权限不足");
            throw new Http_403_ForbiddenException(message);
        }
        log.info("权限校验通过，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
    }
}