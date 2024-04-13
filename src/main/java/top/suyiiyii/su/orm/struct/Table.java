package top.suyiiyii.su.orm.struct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 表对象
 * 保存的表名和所包含的字段
 *
 * @author suyiiyii
 */
public class Table {
    public String tableName;
    public List<Column> columns = new ArrayList<>();

    public <T> Table(String tableName, Class<T> clazz) {
        this.tableName = tableName;
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Column column = new Column(field);
            columns.add(column);
        }
    }

    public String toString() {
        return "Table{" +
                "tableName='" + tableName + '\'' +
                ", columns=" + columns +
                '}';
    }
}
