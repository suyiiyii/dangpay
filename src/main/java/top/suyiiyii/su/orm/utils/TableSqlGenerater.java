package top.suyiiyii.su.orm.utils;


import top.suyiiyii.su.orm.struct.Column;
import top.suyiiyii.su.orm.struct.Table;

/**
 * 生成有关表的sql
 *
 * @author suyiiyii
 */
public class TableSqlGenerater {
    /**
     * 线程安全：全静态方法，无静态变量，不涉及线程安全问题
     */

    public static String getCreateTableSql(Table table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE `" + table.tableName + "` (");
        for (Column column : table.columns) {
            sql.append("\n");
            // 字段名
            sql.append(" `").append(column.name).append("` ");
            // 字段类型
            sql.append(" ").append(column.type);
            if ("varchar".equals(column.type)) {
                sql.append("(").append(column.stringLength).append(")");
            }
            // 是否非空
            if (column.isNotNull) {
                sql.append(" NOT NULL");
            }
            // 是否唯一
            if (column.isUnique) {
                sql.append(" UNIQUE");
            }
            // 是否是主键
            if (column.isPrimaryKey) {
                sql.append(" PRIMARY KEY");
            }
            // 是否自增
            if (column.isAutoIncrement) {
                sql.append(" AUTO_INCREMENT");
            }
            sql.append(",");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(");");
        return sql.toString();
    }

    public static String getDropTableSql(Table table) {
        return "DROP TABLE `" + table.tableName + "`;";
    }

    public static String getIsTableExistSql(Table table) {
        return "SHOW TABLES LIKE '" + table.tableName + "'";
    }
}
