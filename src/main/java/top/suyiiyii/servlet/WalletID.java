package top.suyiiyii.servlet;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Transaction;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.service.WalletService;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;

import java.util.List;

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
        String identity = transactionService.createMoneyReceiveIdentity(subMethod.getId(), dto.isSpecifiedAmount, dto.amount, dto.description);
        return configManger.get("BASE_URL") + "/api/requestTransaction?identity=" + identity;
    }

    public TransactionService.ScanQRCodeResponse doPostScanQRCode(HttpServletRequest req, HttpServletResponse resp) {
        ScanQRCodeRequest scanQRCodeRequest = WebUtils.readRequestBody2Obj(req, ScanQRCodeRequest.class);
        return transactionService.scanQRCode(subMethod.getId(), scanQRCodeRequest.getCallbackUrl());
    }

    @Data
    public static class ScanQRCodeRequest {
        String callbackUrl;
    }

    @Data
    public static class createIdentityDto {
        //这个错误是由于在尝试将JSON数据映射到createIdentityDto类时，Jackson找不到名为isSpecifiedAmount的属性。在Java中，布尔类型的getter方法通常以is开头，而setter方法则没有is。因此，Jackson期望找到的属性名是specifiedAmount，而不是isSpecifiedAmount。
        @JsonProperty("isSpecifiedAmount")
        boolean isSpecifiedAmount;
        int amount;
        String description;
    }

    @Data
    public static class allocateDto {
        int id;
        int amount;
    }


}
