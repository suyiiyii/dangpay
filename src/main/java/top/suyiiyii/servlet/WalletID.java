package top.suyiiyii.servlet;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Transaction;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.service.WalletService;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;
import top.suyiiyii.su.validator.Regex;

import java.util.List;

@Slf4j
public class WalletID {
    private final GroupService groupService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final ConfigManger configManger;
    private IngressServlet.SubMethod subMethod;

    public WalletID(GroupService groupService, UserRoles userRoles, WalletService walletService, IngressServlet.SubMethod subMethod, TransactionService transactionService, ConfigManger configManger) {
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.subMethod = subMethod;
        this.configManger = configManger;
    }

    /**
     * deprecated
     * 使用groupID 内的 allocate 方法代替
     * @param req
     * @param resp
     * @return
     */
    public boolean doPostAllocate(HttpServletRequest req, HttpServletResponse resp) {
        allocateDto idDto = WebUtils.readRequestBody2Obj(req, allocateDto.class);
        walletService.allocate(subMethod.getId(), idDto.id, idDto.amount);
        return true;
    }

    public Wallet doGet(HttpServletRequest req, HttpServletResponse resp) {
        return walletService.getWallet(subMethod.getId());
    }

    public List<Transaction> doGetTransactions(HttpServletRequest req, HttpServletResponse resp) {
        return walletService.getWalletTransactions(subMethod.getId());
    }

    public String doPostCreateReceiveIdentity(HttpServletRequest req, HttpServletResponse resp) {
        createIdentityDto dto = WebUtils.readRequestBody2Obj(req, createIdentityDto.class);
        String identity = transactionService.createMoneyReceiveIdentity(subMethod.getId(), dto.isAmountSpecified, dto.amount, dto.description);
        String callbackUrl = configManger.get("BASE_URL") + "/api/requestTransaction?identity=" + identity;
        log.debug("创建收款标识，钱包ID：" + subMethod.getId() + "，回调地址：" + callbackUrl);
        return callbackUrl;
    }

    public TransactionService.ScanQRCodeResponse doPostScanQRCode(HttpServletRequest req, HttpServletResponse resp) {
        ScanQRCodeRequest scanQRCodeRequest = WebUtils.readRequestBody2Obj(req, ScanQRCodeRequest.class);
        log.debug("收到扫码请求，钱包ID：" + subMethod.getId() + "，回调地址：" + scanQRCodeRequest.getCallbackUrl() + "，开始扫码");
        return transactionService.scanQRCode(subMethod.getId(), scanQRCodeRequest.getCallbackUrl());
    }

    public TransactionService.UserPayResponse doPostPay(HttpServletRequest req, HttpServletResponse resp) {
        TransactionService.UserPayRequest userPayRequest = WebUtils.readRequestBody2Obj(req, TransactionService.UserPayRequest.class);
        log.debug("收到用户支付请求，钱包ID：" + subMethod.getId() + "，用户ID：" + userRoles.getUid() + "，开始支付");
        return transactionService.userPay(subMethod.getId(), userRoles.getUid(), userPayRequest);
    }


    @Data
    public static class ScanQRCodeRequest {
        @Regex("https?://.*api/requestTransaction\\?identity=.*")
        String callbackUrl;
    }

    @Data
    public static class createIdentityDto {
        //这个错误是由于在尝试将JSON数据映射到createIdentityDto类时，Jackson找不到名为isSpecifiedAmount的属性。在Java中，布尔类型的getter方法通常以is开头，而setter方法则没有is。因此，Jackson期望找到的属性名是specifiedAmount，而不是isSpecifiedAmount。
        @JsonProperty("isAmountSpecified")
        boolean isAmountSpecified;
        int amount;
        String description;
    }

    @Data
    public static class allocateDto {
        int id;
        int amount;
    }


}
