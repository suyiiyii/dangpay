package top.suyiiyii.su.IOC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用方法
 * 只要是带了这个注解的类，都会被代理
 * 会不会权限校验，是否需要事务，取决于这个注解的配置
 * 设置isNotProxy为true，可以强制阻止代理
 */
@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxy {
    boolean isNeedAuthorization() default true;

    boolean transaction() default false;

    boolean isNotProxy() default false;

    String subId() default "";
}
