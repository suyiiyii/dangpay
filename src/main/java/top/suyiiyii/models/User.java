package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;
import top.suyiiyii.su.validator.Regex;

@Data
@TableRegister("user")
public class User {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isUnique = true, stringLength = 100, isNotNull = true)
    @Regex("^[a-zA-Z0-9_-]{3,16}$")
    String username;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String password;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    @Regex("^1[3-9]\\d{9}$")
    String phone;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String email;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    String iconUrl;
    @ColumnSetting(stringLength = 20, isNotNull = true)
    String status;
}
