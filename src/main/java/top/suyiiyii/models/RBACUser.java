package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("rbac_user")
public class RBACUser {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true)
    int uid;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String role;
}
