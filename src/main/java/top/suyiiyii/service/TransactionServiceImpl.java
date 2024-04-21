package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import top.suyiiyii.dao.TransactionDao;
import top.suyiiyii.models.Transaction;
import top.suyiiyii.models.TransactionIdentity;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Repository
@Proxy(isNeedAuthorization = false)
public class TransactionServiceImpl implements TransactionService {
    Session db;
    RBACService rbacService;
    ConfigManger configManger;
    UserService userService;
    TransactionDao transactionDao;

    public TransactionServiceImpl(Session db, @Proxy(isNeedAuthorization = false) RBACService rbacService, @Proxy(isNeedAuthorization = false) UserService userService, ConfigManger configManger, TransactionDao transactionDao) {
        this.db = db;
        this.userService = userService;
        this.rbacService = rbacService;
        this.configManger = configManger;
        this.transactionDao = transactionDao;
    }

    @Override
    public String createIdentity(int WalletId, boolean isAmountSpecified, int amount, String type, String description) {
        TransactionIdentity transactionIdentity = new TransactionIdentity();
        transactionIdentity.setIdentity("i" + UUID.randomUUID().toString().replace("-", ""));
        transactionIdentity.setWalletId(WalletId);
        if (isAmountSpecified) {
            transactionIdentity.setIsAmountSpecified(1);
            transactionIdentity.setSpecifiedAmount(amount);
        } else {
            transactionIdentity.setIsAmountSpecified(0);
        }
        transactionIdentity.setType(type);
        transactionIdentity.setDescription(description);
        transactionIdentity.setStatus("active");
        transactionIdentity.setCreatedAt(UniversalUtils.getNow());
        transactionIdentity.setUpdatedAt(UniversalUtils.getNow());
        db.insert(transactionIdentity);
        return transactionIdentity.getIdentity();
    }

    @Override
    public String createMoneyReceiveIdentity(int WalletId, boolean isAmountSpecified, int amount, String description) {
        if (amount <= 0) {
            log.error("收款码的金额必须大于0，用户输入的金额：" + amount);
            throw new Http_400_BadRequestException("收款码的金额必须大于0");
        }
        return createIdentity(WalletId, isAmountSpecified, amount, "money_receive", description);
    }


    /**
     * 生成交易code
     * 二维码内存有identity，第三方请求时带上identity，服务器可以查询到这个identity对应的交易信息
     * 然后根据交易信息生成交易code
     *
     * @param identityId identityId
     * @return
     */
    @Override
    public String createCode(int identityId) {

        // 创建交易码
        String transactionCode = generateCode();
        // 构造数据结构
        CodeInCache codeInCache = new CodeInCache();
        codeInCache.setIdentityId(identityId);
        codeInCache.setCode(transactionCode);
        codeInCache.setExpiredAt(UniversalUtils.getNow() + configManger.getInt("TRANSACTION_CODE_EXPIRED_TIME"));
        // 保存code和identity的对应关系
        transactionDao.insertSentCode(transactionCode, codeInCache);
        return transactionCode;
    }

    @Override
    public String generateCode() {
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

    @Override
    public ScanQRCodeResponse scanQRCode(@SubRegion(areaPrefix = "w") int wid, String callbackUrl) {
        // 请求第三方接口，获取交易信息
        // 创建transaction，设置为pending状态，并返回给用户
        OkHttpClient client = new OkHttpClient();
        // 构建请求体
        RequestTransactionRequest requestTransactionRequest = new RequestTransactionRequest();
        requestTransactionRequest.setPlatform(configManger.get("PLATFORM_NAME"));
        requestTransactionRequest.setRequestId(UUID.randomUUID().toString());
        String requestTransactionRequestJson = UniversalUtils.obj2Json(requestTransactionRequest);
        // 给请求体签名
        String sign = UniversalUtils.sign(requestTransactionRequestJson, configManger);

        // 请求第三方平台
        // 创建请求
        Request request = new Request.Builder().url(callbackUrl).post(RequestBody.create(requestTransactionRequestJson.getBytes())).addHeader("X-Signature", sign).build();
        RequestTransactionResponse requestTransactionResponse;
        try {
            // 发送请求
            okhttp3.Response response = client.newCall(request).execute();
            // 获取响应体
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                // 解析响应体
                requestTransactionResponse = UniversalUtils.json2Obj(responseBody, RequestTransactionResponse.class);
                log.debug("请求第三方接口成功，返回信息：" + requestTransactionResponse);
                // 验证签名
                String signature = response.header("X-Signature");
                if (signature == null) {
                    log.error("请求第三方接口失败，签名为空");
                    throw new Http_400_BadRequestException("请求第三方接口失败，签名为空");
                }
                if (!UniversalUtils.verify(responseBody, signature, requestTransactionResponse.getPlatform(), configManger)) {
                    log.error("请求第三方接口失败，签名错误，Platform：" + requestTransactionResponse.getPlatform() + "，X-Signature：" + response.header("X-Signature") + "，ResponseBody：" + responseBody);
                    throw new Http_400_BadRequestException("请求第三方接口失败，签名错误");
                }
                // 储存交易信息
                CodeInCache codeInCache = new CodeInCache();

                transactionDao.insertReceivedCode(requestTransactionResponse.exractCode(), requestTransactionResponse);
            } else {
                log.error("请求第三方接口发生异常：" + response.code() + " " + responseBody);
                throw new Http_400_BadRequestException("请求第三方接口发生异常：" + response.code() + " " + responseBody);
            }
        } catch (Http_400_BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求第三方接口失败", e);
            throw new Http_400_BadRequestException("请求第三方接口失败");
        }

        // 响应用户请求
        // 加密code
        String encryptedCode = UniversalUtils.encrypt(requestTransactionResponse.exractCode(), configManger.get("AES_KEY"));
        // 构建返回信息
        ScanQRCodeResponse scanQRCodeResponse = new ScanQRCodeResponse();
        scanQRCodeResponse.setCode(encryptedCode);
        scanQRCodeResponse.setMessage(requestTransactionResponse.getMessage());
        scanQRCodeResponse.setPlatform(requestTransactionResponse.getPlatform());
        scanQRCodeResponse.isAmountSpecified = requestTransactionResponse.isAmountSpecified();
        scanQRCodeResponse.specifiedAmount = requestTransactionResponse.getSpecifiedAmount();
        scanQRCodeResponse.setExpiredAt(requestTransactionResponse.getExpiredAt());
        scanQRCodeResponse.setRequestId(requestTransactionResponse.getRequestId());


        // 检查用户余额
        Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
        if (wallet.getAmount() < requestTransactionResponse.getSpecifiedAmount()) {
            log.error("用户余额不足 余额：" + wallet.getAmount() + "，交易金额：" + requestTransactionResponse.getSpecifiedAmount());
            scanQRCodeResponse.setMessage("用户余额不足");
            scanQRCodeResponse.setCode("");
        }

        return scanQRCodeResponse;
    }

    @Override
    public RequestTransactionResponse requestTransaction(String identity, RequestTransactionRequest request) {
        // 查询交易信息
        TransactionIdentity transactionIdentity = db.query(TransactionIdentity.class).eq("identity", identity).first();
        // 生成交易码
        String code = createCode(transactionIdentity.getId());

        // 构建返回信息
        RequestTransactionResponse response = new RequestTransactionResponse();
        response.status = "success";
        response.message = "向 " + configManger.get("PLATFORM_NAME") + " 平台的 " + transactionIdentity.getWalletId() + " 转账 " + transactionIdentity.getSpecifiedAmount() + " 元";
        response.platform = configManger.get("PLATFORM_NAME");
        response.callback = configManger.get("BASE_URL") + "/api/startTransaction?code=" + code;
        response.isAmountSpecified = transactionIdentity.getIsAmountSpecified() == 1;
        response.specifiedAmount = transactionIdentity.getSpecifiedAmount();
        response.expiredAt = UniversalUtils.getNow() + 60 * 5;
        response.requestId = request.getRequestId();
        log.info("收到交易请求，identity：" + identity + "，生成code：" + code + "返回回调接口： " + response.callback);
        return response;
    }


    /**
     * 确认交易
     * 用户获得扫码结构之后，请求确认交易接口，服务器解密code，查询到对应的identity，然后创建transaction，设置为frozen状态，向第三方平台发起支付请求
     */
    @Override
    @Proxy(isTransaction = true)
    public UserPayResponse userPay(@SubRegion(lockKey = "w") int wid, int uid, UserPayRequest userPayRequest) {
        // 验证用户身份
        if (!userService.checkPassword(userPayRequest.getPassword(), uid)) {
            log.error("用户支付失败，密码错误");
            throw new Http_400_BadRequestException("密码错误");
        }
        // 解密code
        String code = UniversalUtils.decrypt(userPayRequest.getCode(), configManger.get("AES_KEY"));
        // 查询交易信息
        RequestTransactionResponse requestTransactionResponse;
        try {
            requestTransactionResponse = transactionDao.getReceivedCode(code);
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("code不存在或已过期");
        }

        // 创建transaction
        Transaction transaction = new Transaction();
        transaction.setWalletId(wid);
        transaction.setAmount(requestTransactionResponse.isAmountSpecified ? requestTransactionResponse.getSpecifiedAmount() : 0);
        transaction.setType("online_pay");
        transaction.setStatus("frozen");
        transaction.setCreateTime(UniversalUtils.getNow());
        transaction.setLastUpdate(UniversalUtils.getNow());
        transaction.setPlatform(requestTransactionResponse.getPlatform());
        transaction.setDescription("向 " + requestTransactionResponse.getPlatform() + " 平台的 " + wid + " 转账 " + transaction.getAmount() + " 元");
        db.insert(transaction);
        log.info("用户身份成功，创建transaction：" + transaction);
        // 冻结用户资金
        Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
        wallet.setAmount(wallet.getAmount() - transaction.getAmount());
        wallet.setAmountInFrozen(wallet.getAmountInFrozen() + transaction.getAmount());
        wallet.setLastUpdate(UniversalUtils.getNow());
        db.update(wallet);


        // 向第三方平台发起支付请求
        // 构建发往第三方平台的交易信息
        StartTransactionRequest startTransactionRequest = new StartTransactionRequest();
        startTransactionRequest.setPlatform(requestTransactionResponse.getPlatform());
        startTransactionRequest.setTradeDescription(requestTransactionResponse.getMessage());
        startTransactionRequest.setPayeeName(configManger.get("PLATFORM_NAME"));
        startTransactionRequest.setAmount(requestTransactionResponse.isAmountSpecified ? requestTransactionResponse.getSpecifiedAmount() : 0);
        startTransactionRequest.setRequestId(requestTransactionResponse.getRequestId());

        // 构建请求体
        String startTransactionRequestJson = UniversalUtils.obj2Json(startTransactionRequest);
        // 给请求体签名
        String sign = UniversalUtils.sign(startTransactionRequestJson, configManger);
        // 请求第三方平台
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(requestTransactionResponse.getCallback()).post(RequestBody.create(startTransactionRequestJson.getBytes())).addHeader("X-Signature", sign).build();
        try {
            // 发送请求
            okhttp3.Response response = client.newCall(request).execute();
            // 获取响应体
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                // 解析响应体
                StartTransactionResponse startTransactionResponse = UniversalUtils.json2Obj(responseBody, StartTransactionResponse.class);
                log.debug("请求第三方接口成功，返回信息：" + startTransactionResponse);
                // 验证签名
                String signature = response.header("X-Signature");
                if (signature == null) {
                    log.error("请求第三方接口失败，签名为空");
                    throw new Http_400_BadRequestException("请求第三方接口失败，签名为空");
                }
                if (!UniversalUtils.verify(responseBody, signature, requestTransactionResponse.getPlatform(), configManger)) {
                    log.error("请求第三方接口失败，签名错误，Platform：" + requestTransactionResponse.getPlatform() + "，X-Signature：" + response.header("X-Signature") + "，ResponseBody：" + responseBody);
                    throw new Http_400_BadRequestException("请求第三方接口失败，签名错误");
                }
                // 响应ack
                String ackUrl = startTransactionResponse.getCallback();
                // 构建请求
                Request ackRequest = new Request.Builder().url(ackUrl).post(RequestBody.create("", MediaType.parse("text/plain"))).build();
                // 发送请求
                okhttp3.Response ackResponse = client.newCall(ackRequest).execute();

                // 更新transaction状态
                transaction.setStatus("success");
                transaction.setLastUpdate(UniversalUtils.getNow());
                db.update(transaction);
                // 更新wallet
                wallet.setAmountInFrozen(wallet.getAmountInFrozen() - transaction.getAmount());
                wallet.setLastUpdate(UniversalUtils.getNow());
            } else {
                log.error("请求第三方接口发生异常：" + response.code() + " " + responseBody);
                throw new Http_400_BadRequestException("请求第三方接口发生异常：" + response.code() + " " + responseBody);
            }
        } catch (Http_400_BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求第三方接口失败", e);
            throw new Http_400_BadRequestException("请求第三方接口失败");
        }

        // 构建返回信息
        UserPayResponse userPayResponse = new UserPayResponse();
        userPayResponse.setStatus("success");
        userPayResponse.setMessage("支付成功");
        userPayResponse.setRequestId(userPayRequest.getRequestId());
        return userPayResponse;
    }

    /**
     * 其他平台向本平台发起交易请求
     *
     * @param code    交易code
     * @param request 请求体
     * @return 响应体
     */
    @Override
    @Proxy(isTransaction = true)
    public StartTransactionResponse startTransaction(String code, StartTransactionRequest request) {
        // 查询缓存的code和identityId的对应关系
        CodeInCache codeInCache;
        try {
            codeInCache = transactionDao.getSentCode(code);
            transactionDao.deleteSentCode(code);
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("code不存在或已过期");
        }

        // 查询交易信息
        TransactionIdentity transactionIdentity = db.query(TransactionIdentity.class).eq("id", codeInCache.getIdentityId()).first();
        // 创建transaction
        Transaction transaction = new Transaction();
        transaction.setWalletId(transactionIdentity.getWalletId());
        transaction.setAmount(transactionIdentity.getIsAmountSpecified() == 1 ? transactionIdentity.getSpecifiedAmount() : 0);
        transaction.setType("online_pay_receive");
        transaction.setStatus("pending");
        transaction.setCreateTime(UniversalUtils.getNow());
        transaction.setLastUpdate(UniversalUtils.getNow());
        transaction.setPlatform(request.getPlatform());
        transaction.setDescription(request.getTradeDescription());
        int transactionId = db.insert(transaction, true);
        log.info("创建transaction：" + transaction);

        // 构建一个ack地址，关连transaction信息
        String ackCode = "ack" + UUID.randomUUID().toString().replace("-", "");
        transactionDao.insert("ack_" + ackCode, String.valueOf(transactionId), Duration.ofMinutes(1));
        // 构建返回信息
        StartTransactionResponse response = new StartTransactionResponse();
        response.setStatus("success");
        response.setMessage("向 " + request.getPlatform() + " 平台的 " + transactionIdentity.getWalletId() + " 转账 " + transaction.getAmount() + " 元");
        response.setCallback(configManger.get("BASE_URL") + "/api/ack?ackCode=" + ackCode);
        response.setRequestId(request.getRequestId());
        return response;
    }

    @Override
    public boolean ack(String ackCode) {
        int transactionId = Integer.parseInt(transactionDao.get("ack_" + ackCode));
        transactionDao.delete("ack_" + ackCode);

        // 更新transaction状态
        Transaction transaction = db.query(Transaction.class).eq("id", transactionId).first();
        transaction.setStatus("success");
        transaction.setLastUpdate(UniversalUtils.getNow());
        // 更新wallet
        Wallet wallet = db.query(Wallet.class).eq("id", transaction.getWalletId()).first();
        wallet.setAmount(wallet.getAmount() + transaction.getAmount());
        wallet.setLastUpdate(UniversalUtils.getNow());

        db.commit();
        return true;
    }

}
