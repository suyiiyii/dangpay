package top.suyiiyii.su.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 字段的更多信息的注解
 *
 * @author suyiiyii
 */
@Target(ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ColumnSetting {
    boolean isNotNull() default false;

    boolean isUnique() default false;

    boolean isPrimaryKey() default false;

    boolean isAutoIncrement() default false;

    int stringLength() default 200;
}
