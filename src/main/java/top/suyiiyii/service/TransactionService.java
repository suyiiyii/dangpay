package top.suyiiyii.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import top.suyiiyii.models.Transaction;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.SubRegion;

import java.util.List;

@Proxy(isNeedAuthorization = true)
public interface TransactionService {
    String createIdentity(int WalletId, boolean isAmountSpecified, int amount, String type, String description);

    String createMoneyReceiveIdentity(@SubRegion(areaPrefix = "w") int wid, boolean isAmountSpecified, int amount, String description);

    ScanQRCodeResponse scanQRCode(@SubRegion(areaPrefix = "w") int wid, String callbackUrl);

    RequestTransactionResponse requestTransaction(String identity, RequestTransactionRequest request);

    UserPayResponse userPay(@SubRegion(areaPrefix = "w") int wid, int uid, UserPayRequest userPayRequest);

    StartTransactionResponse startTransaction(String code, StartTransactionRequest request);

    boolean ack(String ackCode);


    List<Transaction> getAllTransactions(int page, int size);

    @Data
    class UserPayRequest {
        public String code;
        public String password;
        public String requestId;
    }

    @Data
    class UserPayResponse {
        public String status;
        public String message;
        public String requestId;
    }

    @Data
    class RequestTransactionRequest {
        public String platform;
        public String requestId;
    }

    /**
     * 用于保存在redis中的code和identity的对应关系
     */
    @Data
    class CodeInCache {
        public int identityId;
        public String code;
        public int expiredAt;
    }

    @Data
    class RequestTransactionResponse {
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
    class ScanQRCodeResponse {
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
    class StartTransactionRequest {
        public String platform;
        public String tradeDescription;
        public String payeeName;
        public int amount;
        public String requestId;
    }

    @Data
    class StartTransactionResponse {
        public String status;
        public String message;
        public String callback;
        public String requestId;
    }
}
