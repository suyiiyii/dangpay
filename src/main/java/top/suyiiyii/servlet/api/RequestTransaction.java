package top.suyiiyii.servlet.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;

@Slf4j
public class RequestTransaction {
    private final TransactionService transactionService;
    private final ConfigManger configManger;

    public RequestTransaction(TransactionService transactionService,
                              ConfigManger configManger) {
        this.transactionService = transactionService;
        this.configManger = configManger;
    }

    public TransactionService.requestTransactionResponse doPost(HttpServletRequest req, HttpServletResponse resp) {
        String requestBody = WebUtils.readRequestBody(req);
        TransactionService.requestTransactionRequest request = UniversalUtils.json2Obj(requestBody, TransactionService.requestTransactionRequest.class);
        String sign = req.getHeader("X-Signature");
        // 验证签名
        if (!UniversalUtils.verify(requestBody, sign, request.getPlatform(), configManger)) {
            log.error("签名验证失败");
            throw new RuntimeException("签名验证失败");
        }

        // 生成交易码
        String identity = req.getParameter("identity");
        String code = transactionService.createCode(identity);
        // 构建返回信息
        TransactionService.requestTransactionResponse response = new TransactionService.requestTransactionResponse();
        response.status = "success";
        response.message = "交易码已生成";
        response.platform = configManger.get("PLATFORM_NAME");
        response.callback = configManger.get("BASE_URL") + "/api/startTransaction?code=" + code;
        response.isSpecifiedAmount = false;
        response.expiredAt = 0;
        response.requestId = request.getRequestId();
        log.info("收到交易请求，identity：" + identity + "，生成code：" + code + "返回回调接口： " + response.callback);
        //TODO 过期时间需要设置

        // 添加签名
        String responseBody = UniversalUtils.obj2Json(response);
        resp.setHeader("X-Signature", UniversalUtils.sign(responseBody, configManger));
        return response;
    }

}
