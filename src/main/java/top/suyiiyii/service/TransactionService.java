package top.suyiiyii.service;

import top.suyiiyii.models.TransactionCode;
import top.suyiiyii.models.TransactionIdentity;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.NoSuchElementException;
import java.util.UUID;

public class TransactionService {
    Session db;
    RBACService rbacService;
    ConfigManger configManger;
    UserService userService;

    public TransactionService(Session db, @Proxy(isNeedAuthorization = false) RBACService rbacService, @Proxy(isNeedAuthorization = false) UserService userService, ConfigManger configManger) {
        this.db = db;
        this.userService = userService;
        this.rbacService = rbacService;
        this.configManger = configManger;
    }

    private String createIdentity(int WalletId, boolean isSpecifiedAmount, int amount, String type, String description) {
        TransactionIdentity transactionIdentity = new TransactionIdentity();
        transactionIdentity.setIdentity("i" + UUID.randomUUID().toString().replace("-", ""));
        transactionIdentity.setWalletId(WalletId);
        if (isSpecifiedAmount) {
            transactionIdentity.setIsSpecifiedAmount(1);
            transactionIdentity.setSpecifiedAmount(amount);
        } else {
            transactionIdentity.setIsSpecifiedAmount(0);
        }
        transactionIdentity.setType(type);
        transactionIdentity.setDescription(description);
        transactionIdentity.setStatus("active");
        transactionIdentity.setCreatedAt(UniversalUtils.getNow());
        transactionIdentity.setUpdatedAt(UniversalUtils.getNow());
        db.insert(transactionIdentity, true);
        return transactionIdentity.getIdentity();
    }

    public String createMoneyReceiveIdentity(int WalletId, boolean isSpecifiedAmount, int amount, String description) {
        return createIdentity(WalletId, isSpecifiedAmount, amount, "money_receive", description);
    }

    /**
     * 生成交易
     *
     * @param identity 交易标识id
     * @return 交易码
     */
    @Proxy(isTransaction = true)
    public String createCode(String identity) {
        // 找到对应的交易标识
        int identityId;
        try {
            TransactionIdentity identity1 = db.query(TransactionIdentity.class).eq("identity", identity).first();
            identityId = identity1.getId();
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("交易标识不存在");
        }
        // 创建交易码
        TransactionCode transactionCode = new TransactionCode();
        transactionCode.setIdentityId(identityId);
        transactionCode.setCode(generateCode());
        transactionCode.setExpiredAt(UniversalUtils.getNow());
        db.insert(transactionCode, true);
        return transactionCode.getCode();
    }

    private String generateCode() {
        return UUID.randomUUID().toString();
    }

}
