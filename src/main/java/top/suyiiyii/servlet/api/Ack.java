package top.suyiiyii.servlet.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.su.ConfigManger;

public class Ack {

    private final TransactionService transactionService;
    private final ConfigManger configManger;

    public Ack(TransactionService transactionService,
               ConfigManger configManger) {
        this.transactionService = transactionService;
        this.configManger = configManger;
    }

    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        String ackCode = req.getParameter("ackCode");
        transactionService.ack(ackCode);
        return true;
    }
}
