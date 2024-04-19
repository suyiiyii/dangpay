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
