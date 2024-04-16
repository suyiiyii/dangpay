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
        // 在这里添加权限校验的逻辑
        String permission = method.getDeclaringClass().getSimpleName() + UniversalUtils.capitalizeFirstLetter(method.getName());
        int subId = 0;
        if (method.isAnnotationPresent(RBACAuthorization.class)) {
            RBACAuthorization annotation = method.getAnnotation(RBACAuthorization.class);
            // 如果方法上的注解标记为不需要权限校验，则直接返回
            if (!annotation.isNeedAuthorization()) {
                return;
            }
            // 如果方法上的注解标记了权限id的字段，则获取该字段的值
            if (!annotation.subId().isEmpty()) {
                subId = -1;
                // 因为代理对象代理的是接口，所以无法通过反射获取参数名，只能通过参数类型来判断
                // 从参数中获取subId的值，默认为第一个int类型的参数
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].getType().equals(int.class)) {
                        subId = (int) args[i];
                        break;
                    }
                }
            }
        }
        boolean result = rbacService.checkUserPermission(userRoles, permission, subId);
        if (!result) {
            String message = "权限校验失败，请求用户: " + userRoles.uid + " 用户角色: " + userRoles.roles + " 请求权限: " + permission;
            log.info("权限校验失败，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
//            throw new Http_403_ForbiddenException("权限不足");
            throw new Http_403_ForbiddenException(message);
        }
        log.info("权限校验通过，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
    }
}