package top.suyiiyii.su.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 注解，用于标记实体类对应的表名
 *
 * @author suyiiyii
 */
@Target(ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface TableRegister {
    String value();
}
