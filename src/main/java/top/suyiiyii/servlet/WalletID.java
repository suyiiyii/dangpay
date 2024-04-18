package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.WalletService;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;

public class WalletID {
    private final GroupService groupService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    private IngressServlet.SubMethod subMethod;

    public WalletID(GroupService groupService, UserRoles userRoles, @Proxy(isNeedAuthorization = false) WalletService walletService, IngressServlet.SubMethod subMethod) {
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.walletService = walletService;
        this.subMethod = subMethod;

    }

    public boolean doPostAllocate(HttpServletRequest req, HttpServletResponse resp) {
        allocateDto idDto = WebUtils.readRequestBody2Obj(req, allocateDto.class);
        walletService.allocate(subMethod.getId(), idDto.id, idDto.amount);
        return true;
    }

    @Data
    public static class allocateDto {
        int id;
        int amount;
    }


}
