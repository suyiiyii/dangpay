package top.suyiiyii.su.IOC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository注解
 * 用于标记一个类为实现类
 * 当遇到接口时，会自动寻找对应的实现类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {
}
