package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("group_member")
public class GroupMember {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true)
    int groupId;
    @ColumnSetting(isNotNull = true)
    int userId;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String role;
}
