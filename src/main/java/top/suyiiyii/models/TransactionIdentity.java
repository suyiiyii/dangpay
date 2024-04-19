package top.suyiiyii.models;


import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

/**
 * TransactionIdentity
 * 交易标识
 * 用于记录交易的基本信息，记录在二维码中
 */
@Data
@TableRegister("transaction_identity")
public class TransactionIdentity {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String identity;
    @ColumnSetting(stringLength = 20)
    String type;
    @ColumnSetting(stringLength = 250)
    String description;
    @ColumnSetting(stringLength = 20)
    String status;
    @ColumnSetting(isNotNull = true)
    int walletId;
    int isSpecifiedAmount;
    int specifiedAmount;
    int createdAt;
    int updatedAt;
}
