package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("Event")
public class Event {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true)
    int uid;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String method;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String permission;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String ip;
    @ColumnSetting(isNotNull = true, stringLength = 1000)
    String ua;
    @ColumnSetting(isNotNull = true)
    int createTime;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String status;
}
