package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("rbac_role")
public class RBACRole {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String role;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String permission;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String status;
    @ColumnSetting(isNotNull = true)
    int createTime;
}
