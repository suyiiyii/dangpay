package top.suyiiyii.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import top.suyiiyii.dao.TransactionDao;
import top.suyiiyii.models.TransactionCode;
import top.suyiiyii.models.TransactionIdentity;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
public class TransactionService {
    Session db;
    RBACService rbacService;
    ConfigManger configManger;
    UserService userService;
    TransactionDao transactionDao;

    public TransactionService(Session db,
                              @Proxy(isNeedAuthorization = false) RBACService rbacService,
                              @Proxy(isNeedAuthorization = false) UserService userService,
                              ConfigManger configManger,
                              TransactionDao transactionDao) {
        this.db = db;
        this.userService = userService;
        this.rbacService = rbacService;
        this.configManger = configManger;
        this.transactionDao = transactionDao;
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
     * <p>
     * <p>
     * 由于code不能直接暴露给用户，但服务器又是无状态的，为了避免在数据库中存储code和identity的对应关系，这里采用了一种比较巧妙的方法：把code加密后发送用户，用户继续交易时带上加密后的code，服务器解密后即可查到对应的identity
     */

    public ScanQRCodeResponse scanQRCode(@SubRegion(areaPrefix = "w") int wid, String callbackUrl) {
        // 请求第三方接口，获取交易信息
        // 创建transaction，设置为pending状态，并返回给用户
        OkHttpClient client = new OkHttpClient();
        // 构建请求体
        requestTransactionRequest requestTransactionRequest = new requestTransactionRequest();
        requestTransactionRequest.setPlatform(configManger.get("PLATFORM_NAME"));
        requestTransactionRequest.setRequestId(UUID.randomUUID().toString());
        String requestTransactionRequestJson = UniversalUtils.obj2Json(requestTransactionRequest);
        // 给请求体签名
        String sign = UniversalUtils.rsaSign(requestTransactionRequestJson, configManger.get("ID_RSA"));
        // 创建请求
        Request request = new Request.Builder()
                .url(callbackUrl)
                .post(RequestBody.create(requestTransactionRequestJson.getBytes()))
                .addHeader("X-Signature", sign)
                .build();
        // 发送请求
        requestTransactionResponse requestTransactionResponse;
        try {
            okhttp3.Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                // 获取响应体
                String responseBody = response.body().string();
                // 解析响应体
                requestTransactionResponse = UniversalUtils.json2Obj(responseBody, TransactionService.requestTransactionResponse.class);
                log.debug("请求第三方接口成功，返回信息：" + requestTransactionResponse);
                // 储存交易信息
                transactionDao.createReceivedCode(requestTransactionResponse.exractCode(), requestTransactionResponse);
            } else {
                throw new Http_400_BadRequestException("请求第三方接口失败，网络错误");
            }
        } catch (Exception e) {
            throw new Http_400_BadRequestException("请求第三方接口失败");
        }
        // 加密code
        String encryptedCode = UniversalUtils.encrypt(requestTransactionResponse.exractCode(), configManger.get("AES_KEY"));
        // 构建返回信息
        ScanQRCodeResponse scanQRCodeResponse = new ScanQRCodeResponse();
        scanQRCodeResponse.setCode(encryptedCode);
        scanQRCodeResponse.setMessage(requestTransactionResponse.getMessage());
        scanQRCodeResponse.setPlatform(requestTransactionResponse.getPlatform());
        scanQRCodeResponse.isSpecifiedAmount = requestTransactionResponse.isSpecifiedAmount();
        scanQRCodeResponse.specifiedAmount = requestTransactionResponse.getSpecifiedAmount();
        scanQRCodeResponse.setExpiredAt(requestTransactionResponse.getExpiredAt());
        return scanQRCodeResponse;
    }

    @Data
    public static class requestTransactionRequest {
        public String platform;
        public String requestId;
    }

    @Data
    public static class requestTransactionResponse {
        public String status;
        public String message;
        public String platform;
        public String callback;
        public boolean isSpecifiedAmount;
        public int specifiedAmount;
        public int expiredAt;
        public String requestId;

        public String exractCode() {
            return callback.split("code=")[1];
        }
    }

    @Data
    public static class ScanQRCodeResponse {
        public String code;
        public String message;
        public String platform;
        public boolean isSpecifiedAmount;
        public int specifiedAmount;
        public int expiredAt;
        public String requestId;
    }


}
