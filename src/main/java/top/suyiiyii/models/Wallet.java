package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

@Data
@TableRegister("wallet")
public class Wallet {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    @ColumnSetting(stringLength = 450, isNotNull = true)
    String name;
    @ColumnSetting(isNotNull = true)
    int amount;
    @ColumnSetting(isNotNull = true)
    int amountInFrozen;
    @ColumnSetting(stringLength = 20, isNotNull = true)
    String ownerType;
    @ColumnSetting(isNotNull = true)
    int ownerId;
    @ColumnSetting(isNotNull = true)
    int isSubWallet;
    @ColumnSetting(isNotNull = true)
    int fatherWalletId;
    @ColumnSetting(isNotNull = true)
    int lastUpdate;
    @ColumnSetting(isNotNull = true, stringLength = 20)
    String status;
}

