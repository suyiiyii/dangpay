package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.WalletService;
import top.suyiiyii.su.WebUtils;

import java.util.List;

public class Wallet {
    private final GroupService groupService;
    private final UserRoles userRoles;
    private final WalletService walletService;

    public Wallet(GroupService groupService, UserRoles userRoles, WalletService walletService) {
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.walletService = walletService;
    }


    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        Type type = WebUtils.readRequestBody2Obj(req, Type.class);
        switch (type.getType()) {
            case "createPersonalWallet":
                walletService.createPersonalWallet(userRoles.getUid());
                break;
            case "createGroupWallet":
                walletService.createGroupWallet(type.getId());
                break;
            case "createSubWallet": // deprecated
                walletService.createSubWallet(type.getId(), type.getId2());
                break;
        }
        return true;
    }

    public List<top.suyiiyii.models.Wallet> doGet(HttpServletRequest req, HttpServletResponse resp) {
        return walletService.getMyWallets(userRoles.getUid());
    }

    @Data
    public static class Type {
        String type;
        int id;
        int id2;
    }
}
