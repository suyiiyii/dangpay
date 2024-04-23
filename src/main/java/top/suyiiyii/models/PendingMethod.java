package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("pending_method")
public class PendingMethod {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String method;
    @ColumnSetting(isNotNull = true, stringLength = 5000)
    String args;
    @ColumnSetting(isNotNull = true)
    int applicantId;
    @ColumnSetting(isNotNull = true)
    int approverId;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String reason;
    @ColumnSetting(stringLength = 100)
    String status;
    @ColumnSetting(isNotNull = true)
    int createTime;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String uuid;
}
