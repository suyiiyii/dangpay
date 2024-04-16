package top.suyiiyii.su.validator;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * 参数验证器
 * 通过@Regex 注解标注应该匹配的正则表达式
 * check方法会根据注解进行验证
 */
public class Validator {
    public static void check(Object obj) {
        if (obj == null) {
            throw new RuntimeException("对象不能为空");
        }
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            Regex re = field.getAnnotation(Regex.class);
            // 如果为空则不需要验证
            if (re == null) {
                continue;
            }
            String regex = re.value();
            Pattern pattern = Pattern.compile(regex);
            if (re != null) {
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(obj);
                    if (!pattern.matcher(value).matches()) {
                        throw new IllegalArgumentException();
                    }
                } catch (NullPointerException | IllegalAccessException | IllegalArgumentException e) {
                    throw new IllegalArgumentException("参数 " + field.getName() + " 不合法，需要匹配 " + regex);
                }
            }
        }

    }
}
