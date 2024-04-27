package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

/**
 * 交易记录表
 * 只要金额数字变动，就会记录一条交易记录
 * 交易记录不可修改，删除
 */
@Data
@TableRegister("transaction")
public class Transaction {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true)
    int walletId;
    @ColumnSetting(isNotNull = true)
    int amount;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String type;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String status;
    @ColumnSetting(isNotNull = true)
    int relateUserId;
    @ColumnSetting(isNotNull = true)
    int createTime;
    @ColumnSetting(isNotNull = true)
    int lastUpdate;
    @ColumnSetting(isNotNull = true, stringLength = 200)
    String platform;
    @ColumnSetting(isNotNull = true, stringLength = 200)
    String description;
    @ColumnSetting(isNotNull = true, stringLength = 200)
    String reimburse;


}
