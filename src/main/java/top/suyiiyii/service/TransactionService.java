package top.suyiiyii.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.SubRegion;

@Proxy
public interface TransactionService {
    String createIdentity(int WalletId, boolean isAmountSpecified, int amount, String type, String description);

    String createMoneyReceiveIdentity(int WalletId, boolean isAmountSpecified, int amount, String description);

    @Proxy(isTransaction = true)
    String createCode(int identityId);

    String generateCode();

    ScanQRCodeResponse scanQRCode(@SubRegion(areaPrefix = "w") int wid, String callbackUrl);

    RequestTransactionResponse requestTransaction(String identity, RequestTransactionRequest request);

    @Proxy(isTransaction = true)
    UserPayResponse userPay(int wid, int uid, UserPayRequest userPayRequest);

    StartTransactionResponse startTransaction(String code, StartTransactionRequest request);

    boolean ack(String ackCode);

    @Data
    public static class UserPayRequest {
        public String code;
        public String password;
        public String requestId;
    }

    @Data
    public static class UserPayResponse {
        public String status;
        public String message;
        public String requestId;
    }

    @Data
    public static class RequestTransactionRequest {
        public String platform;
        public String requestId;
    }

    /**
     * 用于保存在redis中的code和identity的对应关系
     */
    @Data
    public static class CodeInCache {
        public int identityId;
        public String code;
        public int expiredAt;
    }

    @Data
    public static class RequestTransactionResponse {
        public String status;
        public String message;
        public String platform;
        public String callback;
        @JsonProperty("isAmountSpecified")
        public boolean isAmountSpecified;
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
        @JsonProperty("isAmountSpecified")
        public boolean isAmountSpecified;
        public int specifiedAmount;
        public int expiredAt;
        public String requestId;
    }

    @Data
    public static class StartTransactionRequest {
        public String platform;
        public String tradeDescription;
        public String payeeName;
        public int amount;
        public String requestId;
    }

    @Data
    public static class StartTransactionResponse {
        public String status;
        public String message;
        public String callback;
        public String requestId;
    }
}
