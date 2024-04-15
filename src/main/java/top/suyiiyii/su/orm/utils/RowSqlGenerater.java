package top.suyiiyii.su.orm.utils;


import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.orm.struct.Column;
import top.suyiiyii.su.orm.struct.Table;

/**
 * 有关行的sql生成
 *
 * @author suyiiyii
 */
@Slf4j
public class RowSqlGenerater {
    /**
     * 线程安全：全静态方法，无静态对象，不涉及线程安全问题
     */

    public static String selectByKey(String tableName, String key) {
        return "SELECT * FROM `" + tableName + "` WHERE " + key + " = ?";
    }

    public static String selectAll(String tableName) {
        return "SELECT * FROM `" + tableName + "`";
    }


    public static String getInsertSql(Table table) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        StringBuilder sql2 = new StringBuilder("VALUES (");
        sql.append(" `").append(table.tableName).append("` ").append("(");
        for (int i = 0; i < table.columns.size(); i++) {
            Column column = table.columns.get(i);
            // 跳过自增字段
            if (column.isAutoIncrement) {
                continue;
            }
            sql.append(" `").append(column.name).append("` ").append(",");
            sql2.append("?").append(",");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        sql2.deleteCharAt(sql2.length() - 1);
        sql2.append(")");
        sql.append(" ").append(sql2);
        log.debug("生成的sql: " + sql);
        return sql.toString();
    }

    public static String getUpdateSql(Table table) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(" `").append(table.tableName).append("` ").append(" SET ");
        for (int i = 0; i < table.columns.size(); i++) {
            Column column = table.columns.get(i);
            // 跳过主键字段
            if (column.isPrimaryKey) {
                continue;
            }
            sql.append(column.name).append(" = ?,");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(" WHERE ");
        for (int i = 0; i < table.columns.size(); i++) {
            Column column = table.columns.get(i);
            if (column.isPrimaryKey) {
                sql.append(" `").append(column.name).append("` ").append(" = ?");
                break;
            }
        }
        log.debug("生成的sql: " + sql);
        return sql.toString();
    }


}
