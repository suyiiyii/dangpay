package top.suyiiyii.su.IOC;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_403_ForbiddenException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@RBACAuthorization(isNeedAuthorization = false)
public class AuthorizationInvocationHandler implements InvocationHandler {
    private final Object target;
    private final UserRoles userRoles;
    private final RBACService rbacService;

    public AuthorizationInvocationHandler(Object target, UserRoles userRoles, RBACService rbacService) {
        this.target = target;
        this.userRoles = userRoles;
        this.rbacService = rbacService;
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

        checkAuthorization(method, args);
        try {
            return method.invoke(target, args);
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
        if (method.isAnnotationPresent(RBACAuthorization.class)) {
            RBACAuthorization annotation = method.getAnnotation(RBACAuthorization.class);
            // 如果方法上的注解标记为不需要权限校验，则直接返回
            if (!annotation.isNeedAuthorization()) {
                return;
            }
        }
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