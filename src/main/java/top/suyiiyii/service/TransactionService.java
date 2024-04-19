package top.suyiiyii.service;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import top.suyiiyii.models.Transaction;
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
        transactionCode.setExpiredAt(UniversalUtils.getNow() + configManger.getInt("TRANSACTION_CODE_EXPIRED_TIME"));
        db.insert(transactionCode, true);
        return transactionCode.getCode();
    }

    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    /**
     * 用户提交用户扫码结果接口
     * 1. 用户提交扫码第三方二维码得到的回调地址
     * 2. 平台请求第三方接口，获取交易信息（包括金额，描述之类的）
     * 3. 用户进行身份验证，确认交易
     * 4. 平台创建transaction，设置为frozen状态，冻结用户资金
     * 5. 用之前获取的交易信息，请求第三方接口，发起支付请求
     * 6. 第三方接口返回支付结果，如果支付成功，设置transaction为success，解冻用户资金
     * 7. 如果支付失败，设置transaction为fail，解冻用户资金
     * 8. 将交易信息返回给用户
     */
    //TODO
//    public ScanQRCodeResponse scanQRCode(String callbackUrl) {
//        // 请求第三方接口，获取交易信息
//        // 创建transaction，设置为pending状态，并返回给用户
//        OkHttpClient client = new OkHttpClient();
//        // 创建请求
//        Request request = new Request.Builder()
//                .url(callbackUrl)
//                .build();
//        // 发送请求
//        try {
//            okhttp3.Response response = client.newCall(request).execute();
//            if (response.isSuccessful()) {
//                // 获取响应体
//                String responseBody = response.body().string();
//                // 解析响应体
//                requestTransactionResponse requestTransactionResponse = UniversalUtils.json2Obj(responseBody, requestTransactionResponse.class);
//            } else {
//                throw new Http_400_BadRequestException("请求第三方接口失败");
//            }
//        } catch (Exception e) {
//            throw new Http_400_BadRequestException("请求第三方接口失败");
//        }
//        // 创建transaction，设置为pending状态，并返回给用户
//        Transaction transaction = new Transaction();
//
//
//    }

    @Data
    public static class requestTransactionResponse {
        public String status;
        public String message;
        public String platform;
        public String callback;
        public boolean isSpecifiedAmount;
        public int specifiedAmount;
        public int ExpiredAt;
    }

    @Data
    public static class ScanQRCodeResponse {
        public int transactionId;
        public String message;
        public String platform;
        public boolean isSpecifiedAmount;
        public int specifiedAmount;
        public int ExpiredAt;
    }


}
