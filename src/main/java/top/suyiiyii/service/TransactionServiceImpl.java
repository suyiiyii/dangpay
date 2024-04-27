package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class TransactionServiceImpl implements TransactionService {
    Session db;
    RBACService rbacService;
    ConfigManger configManger;
    UserService userService;
    TransactionDao transactionDao;
    WalletService walletService;
    LockService lockService;
    MessageService messageService;

    public TransactionServiceImpl(Session db,
                                  @Proxy(isNeedAuthorization = false) RBACService rbacService,
                                  @Proxy(isNeedAuthorization = false) UserService userService,
                                  ConfigManger configManger, TransactionDao transactionDao,
                                  @Proxy(isNeedAuthorization = false, isNotProxy = true) WalletService walletService,
                                  LockService lockService,
                                  @Proxy(isNeedAuthorization = false) MessageService messageService) {
        this.db = db;
        this.userService = userService;
        this.rbacService = rbacService;
        this.configManger = configManger;
        this.transactionDao = transactionDao;
        this.walletService = walletService;
        this.lockService = lockService;
        this.messageService = messageService;
    }

    @Override
    public String createIdentity(int WalletId, boolean isAmountSpecified, int amount, String type, String description) {
        walletService.checkWalletStatus(WalletId);
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
    public String createMoneyReceiveIdentity(@SubRegion(areaPrefix = "w") int wid, boolean isAmountSpecified, int amount, String description) {
        if (amount <= 0) {
            log.error("收款码的金额必须大于0，用户输入的金额：" + amount);
            throw new Http_400_BadRequestException("收款码的金额必须大于0");
        }
        // 阻止直接使用群组主钱包进行交易
        Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
        if (wallet.getOwnerType().equals("group") && wallet.getOwnerId() == 0) {
            log.error("群组主钱包不允许进行交易");
            throw new Http_400_BadRequestException("群组主钱包不允许进行交易");
        }
        return createIdentity(wid, isAmountSpecified, amount, "money_receive", description);
    }


    /**
     * 生成交易code
     * 二维码内存有identity，第三方请求时带上identity，服务器可以查询到这个identity对应的交易信息
     * 然后根据交易信息生成交易code
     *
     * @param identityId identityId
     * @return
     */
    @Proxy(isTransaction = true)
    private String createCode(int identityId) {

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
        // 检查钱包状态
        walletService.checkWalletStatus(wid);
        // 阻止直接使用群组主钱包进行交易
        Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
        if (wallet.getOwnerType().equals("group") && wallet.getOwnerId() == 0) {
            log.error("群组主钱包不允许进行交易");
            throw new Http_400_BadRequestException("群组主钱包不允许进行交易");
        }
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
        wallet = db.query(Wallet.class).eq("id", wid).first();
        if (wallet.getAmount() < requestTransactionResponse.getSpecifiedAmount()) {
            log.error("用户余额不足 余额：" + wallet.getAmount() + "，交易金额：" + requestTransactionResponse.getSpecifiedAmount());
            scanQRCodeResponse.setMessage("用户余额不足");
            scanQRCodeResponse.setCode("");
        }

        return scanQRCodeResponse;
    }

    @Override
    @Proxy(isTransaction = true)
    public RequestTransactionResponse requestTransaction(String identity, RequestTransactionRequest request) {
        // 查询交易信息
        TransactionIdentity transactionIdentity = db.query(TransactionIdentity.class).eq("identity", identity).first();
        // 判断钱包状态
        walletService.checkWalletStatus(transactionIdentity.getWalletId());
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
//    @Proxy(isTransaction = true)
    public UserPayResponse userPay(int wid, int uid, UserPayRequest userPayRequest) {
        // 验证用户身份
        if (!userService.checkPassword(userPayRequest.getPassword(), uid)) {
            log.error("用户支付失败，密码错误");
            throw new Http_400_BadRequestException("密码错误");
        }
        // 检查钱包状态
        walletService.checkWalletStatus(wid);
        // 解密code
        String code = UniversalUtils.decrypt(userPayRequest.getCode(), configManger.get("AES_KEY"));
        // 查询交易信息
        RequestTransactionResponse requestTransactionResponse;
        try {
            requestTransactionResponse = transactionDao.getReceivedCode(code);
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("code不存在或已过期");
        }

        StartTransactionResult tempResult = doStartTransaction(wid, uid, requestTransactionResponse);

        sendAck(tempResult.startTransactionResponse(), tempResult.transaction(), wid);

        // 构建返回信息
        UserPayResponse userPayResponse = new UserPayResponse();
        userPayResponse.setStatus("success");
        userPayResponse.setMessage("支付成功");
        userPayResponse.setRequestId(userPayRequest.getRequestId());
        return userPayResponse;
    }

    private @NotNull StartTransactionResult doStartTransaction(int wid, int uid, RequestTransactionResponse requestTransactionResponse) {
        if (!requestTransactionResponse.isAmountSpecified) {
            // TODO: 处理未指定金额的情况
            throw new Http_400_BadRequestException("未指定金额的情况");
        }


        Transaction transaction;
        try {

            lockService.tryLock("w" + wid, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
            db.beginTransaction();
            // 创建transaction
            transaction = new Transaction();
            transaction.setWalletId(wid);
            transaction.setAmount(requestTransactionResponse.getSpecifiedAmount());
            transaction.setType("online_pay");
            transaction.setStatus("frozen");
            transaction.setCreateTime(UniversalUtils.getNow());
            transaction.setLastUpdate(UniversalUtils.getNow());
            transaction.setPlatform(requestTransactionResponse.getPlatform());
            transaction.setDescription("向 " + requestTransactionResponse.getPlatform() + " 平台的 " + wid + " 转账 " + transaction.getAmount() + " 元");
            transaction.setRelateUserId(uid);
            transaction.setReimburse("");
            transaction.setId(db.insert(transaction, true));
            // 判断是付款还是收款
            Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
            if (requestTransactionResponse.getSpecifiedAmount() < 0) {
                // 收款
            } else {
                // 检查钱包余额
                if (wallet.getAmount() < requestTransactionResponse.getSpecifiedAmount()) {
                    log.error("钱包余额不足 余额：" + wallet.getAmount() + "，交易金额：" + requestTransactionResponse.getSpecifiedAmount());
                    throw new Http_400_BadRequestException("用户余额不足");
                }
                // 冻结用户资金
                wallet.setAmount(wallet.getAmount() - transaction.getAmount());
                wallet.setAmountInFrozen(wallet.getAmountInFrozen() + transaction.getAmount());
                wallet.setLastUpdate(UniversalUtils.getNow());
            }

            log.info("用户身份认证成功，创建transaction：" + transaction);
            db.update(wallet);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        } finally {
            lockService.unlock("w" + wid);
        }


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
        StartTransactionResponse startTransactionResponse;
        try {
            // 发送请求
            okhttp3.Response response = client.newCall(request).execute();
            // 获取响应体
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                // 解析响应体
                startTransactionResponse = UniversalUtils.json2Obj(responseBody, StartTransactionResponse.class);
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
            } else {
                log.error("请求第三方接口发生异常：" + response.code() + " " + responseBody);
                throw new Http_400_BadRequestException("请求第三方接口发生异常：" + response.code() + " " + responseBody);
            }
        } catch (Exception e) {
            log.error("请求第三方接口失败，开始手动回滚用户余额", e);
            // 手动回滚对数据库的修改
            try {
                lockService.tryLock("t" + transaction.getId(), 10, 10, TimeUnit.SECONDS);
                lockService.tryLock("w" + wid, 10, 10, TimeUnit.SECONDS);
                db.beginTransaction();
                Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
                transaction = db.query(Transaction.class).eq("id", transaction.getId()).first();
                // 判断是付款还是收款
                if (transaction.getAmount() > 0) {
                    wallet.setAmount(wallet.getAmount() + transaction.getAmount());
                    wallet.setAmountInFrozen(wallet.getAmountInFrozen() - transaction.getAmount());
                }
                transaction.setStatus("fail");
                db.commitTransaction();
            } catch (Exception e1) {
                db.rollbackTransaction();
                log.error("数据库手动回滚失败", e1);
                throw e1;
            } finally {
                lockService.unlock("w" + wid);
                lockService.unlock("t" + transaction.getId());
            }
            try {
                throw e;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        StartTransactionResult tempResult = new StartTransactionResult(transaction, startTransactionResponse);
        return tempResult;
    }

    private void sendAck(StartTransactionResponse startTransactionResponse, Transaction transaction, int wid) {
        OkHttpClient client = new OkHttpClient();
        try {
            // ack
            String ackUrl = startTransactionResponse.getCallback();
            // 构建请求
            Request ackRequest = new Request.Builder().url(ackUrl).post(RequestBody.create("", MediaType.parse("text/plain"))).build();
            // 发送请求
            okhttp3.Response ackResponse = client.newCall(ackRequest).execute();
            // 检查ack请求是否成功
            if (ackResponse.isSuccessful()) {
                log.info("ack请求成功");
            } else {
                log.error("ack请求失败：" + ackResponse.code());
                throw new Http_400_BadRequestException("ack请求失败");
            }
        } catch (Exception e) {
            log.error("ack请求失败", e);
            throw new Http_400_BadRequestException("ack请求失败");
        }

        try {
            lockService.tryLock("t" + transaction.getId(), 10, 10, TimeUnit.SECONDS);
            lockService.tryLock("w" + wid, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
            db.beginTransaction();
            Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
            transaction = db.query(Transaction.class).eq("id", transaction.getId()).first();
            // 更新transaction状态
            transaction.setStatus("success");
            transaction.setLastUpdate(UniversalUtils.getNow());
            db.update(transaction);
            // 更新wallet
            if (transaction.getAmount() > 0) {
                // 我方付款，清除用户冻结的资金
                wallet.setAmountInFrozen(wallet.getAmountInFrozen() - transaction.getAmount());
                wallet.setLastUpdate(UniversalUtils.getNow());
            } else {
                // 我方收款，直接增加用户资金
                wallet.setAmount(wallet.getAmount() - transaction.getAmount());
            }
            // 给用户发送通知
            messageService.sendSystemMessage(wallet.getOwnerId(), "交易成功，钱包" + wallet.getId() + "动账" + transaction.getAmount() + "元", "");
            // 如果是群组钱包，给群组的管理员发送通知
            if (wallet.getOwnerType().equals("group")) {
                messageService.sendSystemMessage(wallet.getOwnerId(), "交易成功，钱包" + wallet.getId() + "动账" + transaction.getAmount() + "元", "");
            }
            db.commitTransaction();
        } finally {
            lockService.unlock("w" + wid);
            lockService.unlock("t" + transaction.getId());
        }
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
        transaction.setAmount(transactionIdentity.getIsAmountSpecified() == 1 ? -transactionIdentity.getSpecifiedAmount() : 0);
        transaction.setType("online_pay_receive");
        transaction.setStatus("pending");
        transaction.setCreateTime(UniversalUtils.getNow());
        transaction.setLastUpdate(UniversalUtils.getNow());
        transaction.setPlatform(request.getPlatform());
        transaction.setDescription(request.getTradeDescription());
        transaction.setReimburse("");
        try {

            // 设置关联用户
            lockService.tryLock("w" + transaction.getWalletId(), 10, 10, TimeUnit.SECONDS);
            Wallet wallet = db.query(Wallet.class).eq("id", transactionIdentity.getWalletId()).first();
            transaction.setRelateUserId(wallet.getOwnerId());
            // 检查钱包状态
            walletService.checkWalletStatus(transaction.getWalletId());
            // 判断是付款还是收款
            if (transaction.getAmount() > 0) {
                // 我方付款
                // 检查用户余额
                if (wallet.getAmount() < transaction.getAmount()) {
                    log.error("用户余额不足 余额：" + wallet.getAmount() + "，交易金额：" + transaction.getAmount());
                    throw new Http_400_BadRequestException("用户余额不足");
                }
                // 冻结用户资金
                wallet.setAmount(wallet.getAmount() - transaction.getAmount());
                wallet.setAmountInFrozen(wallet.getAmountInFrozen() + transaction.getAmount());
            }
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
        } finally {
            lockService.unlock("w" + transaction.getWalletId());
        }
    }

    @Override
    @Proxy(isTransaction = true)
    public boolean ack(String ackCode) {
        int transactionId = Integer.parseInt(transactionDao.get("ack_" + ackCode));
        transactionDao.delete("ack_" + ackCode);

        try {
            lockService.tryLock("t" + transactionId, 10, 10, TimeUnit.SECONDS);
            lockService.tryLock("w" + transactionId, 10, 10, TimeUnit.SECONDS);
            // 更新transaction状态
            Transaction transaction = db.query(Transaction.class).eq("id", transactionId).first();
            transaction.setStatus("success");
            transaction.setLastUpdate(UniversalUtils.getNow());
            Wallet wallet = db.query(Wallet.class).eq("id", transaction.getWalletId()).first();
            // 更新wallet
            if (transaction.getAmount() > 0) {
                // 我方付款，清除用户冻结的资金
                wallet.setAmountInFrozen(wallet.getAmountInFrozen() - transaction.getAmount());
                wallet.setLastUpdate(UniversalUtils.getNow());
            } else {
                // 我方收款，直接增加用户资金
                wallet.setAmount(wallet.getAmount() - transaction.getAmount());
            }
            // 给用户发送通知
            messageService.sendSystemMessage(wallet.getOwnerId(), "交易成功，钱包" + wallet.getId() + "动账" + transaction.getAmount() + "元", "");
            // 如果是群组钱包，给群组的管理员发送通知
            if (wallet.getOwnerType().equals("group")) {
                messageService.sendSystemMessage(wallet.getOwnerId(), "交易成功，钱包" + wallet.getId() + "动账" + transaction.getAmount() + "元", "");
            }
            return true;
        } finally {
            lockService.unlock("w" + transactionId);
            lockService.unlock("t" + transactionId);
        }
    }

    @Override
    public List<Transaction> getAllTransactions(int page, int size) {
        return db.query(Transaction.class).limit(page, size).all();
    }

    @Override
    public void setReimburse(int tid, String url, int uid) {
        Transaction transaction = db.query(Transaction.class).eq("id", tid).first();
        if (transaction == null) {
            log.error("交易不存在");
            throw new Http_400_BadRequestException("交易不存在");
        }
        if (transaction.getRelateUserId() != uid) {
            log.error("用户无权操作此交易");
            throw new Http_400_BadRequestException("用户无权操作此交易");
        }
        if (!"".equals(transaction.getReimburse())) {
            log.error("交易已经报销");
            throw new Http_400_BadRequestException("交易已经报销，请勿重复上传");
        }
        transaction.setReimburse(url);
        db.commit();
    }

    private record StartTransactionResult(Transaction transaction, StartTransactionResponse startTransactionResponse) {
    }

}
