package top.suyiiyii.su.IOC;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_403_ForbiddenException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
@RBACAuthorization(isNeedAuthorization = false)
public class AuthorizationInvocationHandler implements InvocationHandler {
    private final Object target;
    private final UserRoles userRoles;
    private final RBACService rbacService;
    private boolean isNeedAuthorization;

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

        if (isNeedAuthorization) {
            // 权限验证
            checkAuthorization(method);
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // invoke方法抛出的是一个包装过的异常，需要通过getTargetException获取原始异常
            throw e.getTargetException();
        }
    }

    public void destroy() {
    }

    public void disableAuthorization() {
        this.isNeedAuthorization = false;
    }

    public void enableAuthorization() {
        this.isNeedAuthorization = true;
    }

    private void checkAuthorization(Method method) {
        // 在这里添加权限校验的逻辑
        String permission = method.getDeclaringClass().getSimpleName() + UniversalUtils.capitalizeFirstLetter(method.getName());
        boolean result = rbacService.checkPermission(userRoles, permission);
        if (!result) {
            log.info("权限校验失败，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
            throw new Http_403_ForbiddenException("权限不足");
        }
        log.info("权限校验通过，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
    }
}