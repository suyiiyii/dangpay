package top.suyiiyii.su.IOC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class AuthorizationInvocationHandler implements InvocationHandler {
    private Object target;

    public AuthorizationInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 权限验证
        checkAuthorization();

        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            // 记录错误信息
            System.err.println("Method invocation failed: " + e.getMessage());
            // 重新抛出异常
            throw e;
        }
    }

    private void checkAuthorization() {
        // 在这里添加权限校验的逻辑
    }

    public static <T> T getProxy(T target) {
        Class<?>[] interfaces = target.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Target object does not implement any interfaces");
        }

        return (T) java.lang.reflect.Proxy.newProxyInstance(target.getClass().getClassLoader(),
                interfaces,
                new AuthorizationInvocationHandler(target));
    }
}