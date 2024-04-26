package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.service.WalletService;

import java.util.List;

public class Transaction {

    private final GroupService groupService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    TransactionService transactionService;

    public Transaction(TransactionService transactionService,
                       GroupService groupService,
                       UserRoles userRoles,
                       WalletService walletService) {
        this.transactionService = transactionService;
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.walletService = walletService;
    }


    public List<top.suyiiyii.models.Transaction> doGet(HttpServletRequest req, HttpServletResponse resp) {
        int page = Integer.parseInt(req.getParameter("page"));
        int size = Integer.parseInt(req.getParameter("size"));
        return transactionService.getAllTransactions(page, size);
    }
}
