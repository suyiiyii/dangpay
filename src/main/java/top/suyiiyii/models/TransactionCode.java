package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

/**
 * TransactionCode
 * 交易码
 * 全局唯一并且有过期时间
 */
@Data
@TableRegister("transaction_code")
public class TransactionCode {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(isNotNull = true)
    int identityId;
    @ColumnSetting(isNotNull = true, stringLength = 100)
    String code;
    @ColumnSetting(isNotNull = true)
    int expiredAt;
}
