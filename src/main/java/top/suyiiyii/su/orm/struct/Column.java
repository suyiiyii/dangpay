package top.suyiiyii.su.orm.struct;

import top.suyiiyii.su.orm.annotation.ColumnSetting;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static top.suyiiyii.su.UniversalUtils.camlToDown;

/**
 * 列对象
 * 代表数据库中的某一列
 * 传入被ColumnSetting注解的字段，可以自动获取额外的信息
 *
 * @author suyiiyii
 */
public class Column {
    // 类型到数据库类型的映射
    private final static Map<String, String> obj2Column = new HashMap<>() {
        {
            put("int", "int");
            put("String", "varchar");
        }
    };
    public String name;
    public String type;
    public boolean isUnique;
    public boolean isPrimaryKey;
    public boolean isAutoIncrement;
    public boolean isNotNull;
    public int stringLength = 255;


    public Column(String name, String type, boolean isPrimaryKey, boolean isAutoIncrement, boolean isNotNull, int stringLength) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isAutoIncrement = isAutoIncrement;
        this.isNotNull = isNotNull;
        this.stringLength = stringLength;
    }

    /**
     * 通过字段对象构造列对象
     *
     * @param field 字段对象
     */
    public Column(Field field) {
        // 获取字段名
        this.name = camlToDown(field.getName());
        // 获取字段类型
        String type = field.getType().getSimpleName();
        this.type = obj2Column.get(type);
        // 判断是否有注解，如果有则获取注解的值
        if (field.isAnnotationPresent(ColumnSetting.class)) {
            ColumnSetting columnSetting = field.getAnnotation(ColumnSetting.class);
            this.isPrimaryKey = columnSetting.isPrimaryKey();
            this.isUnique = columnSetting.isUnique();
            this.isAutoIncrement = columnSetting.isAutoIncrement();
            this.isNotNull = columnSetting.isNotNull();
            this.stringLength = columnSetting.stringLength();
        }

    }

    public String toString() {
        return "Column{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", isPrimaryKey=" + isPrimaryKey + ", isAutoIncrement=" + isAutoIncrement + ", isNotNull=" + isNotNull + ", stringLength=" + stringLength + '}';
    }

}
