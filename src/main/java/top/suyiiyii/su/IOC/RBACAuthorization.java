package top.suyiiyii.su.IOC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用方法
 * 在需要进行权限验证的方法上加上@RBACAuthorization注解，则其他对象注入此方法时会注入一个代理对象
 * 代理对象会在调用方法前进行权限验证
 * 如果不需要进行权限验证，可以在参数上加上@RBACAuthorization(isNeedAuthorization = false)，表示一个例外，不会注入代理对象
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RBACAuthorization {
    boolean isNeedAuthorization() default true;
}
