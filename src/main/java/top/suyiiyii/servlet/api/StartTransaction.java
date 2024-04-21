package top.suyiiyii.servlet.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.service.TransactionServiceImpl;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;

@Slf4j
public class StartTransaction {


    private final TransactionService transactionService;
    private final ConfigManger configManger;

    public StartTransaction(TransactionService transactionService,
                            ConfigManger configManger) {
        this.transactionService = transactionService;
        this.configManger = configManger;
    }

    public TransactionServiceImpl.StartTransactionResponse doPost(HttpServletRequest req, HttpServletResponse resp) {
        // 读取请求体
        String requestBody = WebUtils.readRequestBody(req);
        TransactionServiceImpl.StartTransactionRequest request = UniversalUtils.json2Obj(requestBody, TransactionServiceImpl.StartTransactionRequest.class);
        String sign = req.getHeader("X-Signature");
        // 验证签名
        if (!UniversalUtils.verify(requestBody, sign, request.getPlatform(), configManger)) {
            log.error("签名验证失败");
            throw new RuntimeException("签名验证失败");
        }

        String identity = req.getParameter("code");
        TransactionServiceImpl.StartTransactionResponse response = transactionService.startTransaction(identity, request);

        // 添加签名
        String responseBody = UniversalUtils.obj2Json(response);
        resp.setHeader("X-Signature", UniversalUtils.sign(responseBody, configManger));
        return response;
    }

}
