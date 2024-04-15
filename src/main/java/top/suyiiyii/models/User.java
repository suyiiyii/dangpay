package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("user")
public class User {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String username;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String password;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String phone;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String iconUrl;
}
