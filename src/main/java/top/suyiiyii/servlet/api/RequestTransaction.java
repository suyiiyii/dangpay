package top.suyiiyii.servlet.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.su.ConfigManger;

public class RequestTransaction {
    private final TransactionService transactionService;
    private final ConfigManger configManger;

    public RequestTransaction(TransactionService transactionService,
                              ConfigManger configManger) {
        this.transactionService = transactionService;
        this.configManger = configManger;
    }

    public TransactionService.requestTransactionResponse doPost(HttpServletRequest req, HttpServletResponse resp) {
        String identity = req.getParameter("identity");
        String code = transactionService.createCode(identity);
        TransactionService.requestTransactionResponse response = new TransactionService.requestTransactionResponse();
        response.status = "success";
        response.message = "交易码已生成";
        response.platform = configManger.get("PLATFORM_NAME");
        response.callback = configManger.get("BASE_URL") + "/api/transaction/verify?code=" + code;
        response.isSpecifiedAmount = false;
        response.setExpiredAt(-1);
        //TODO 过期时间需要设置
        return response;
    }

}
