package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

/**
 * 好友表
 * 用户之间可以互相添加好友
 * 好友之间可以互相发送消息
 * 一次添加两条记录，互为好友
 */
@Data
@TableRegister("friend")
public class Friend {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true)
    int uid1;
    @ColumnSetting(isNotNull = true)
    int uid2;
}
