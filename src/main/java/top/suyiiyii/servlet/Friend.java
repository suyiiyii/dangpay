package top.suyiiyii.servlet;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Message;
import top.suyiiyii.service.*;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;

import java.util.List;

public class Friend {
    private GroupService groupService;
    private IngressServlet.SubMethod subMethod;
    private RBACService rbacService;
    private UserRoles userRoles;
    private WalletService walletService;
    private MessageService messageService;
    private FriendService friendService;

    public Friend(GroupService groupService,
                  IngressServlet.SubMethod subMethod,
                  @Proxy(isNeedAuthorization = false) RBACService rbacService,
                  WalletService walletService,
                  MessageService messageService,
                  FriendService friendService,
                  UserRoles userRoles) {
        this.groupService = groupService;
        this.subMethod = subMethod;
        this.rbacService = rbacService;
        this.walletService = walletService;
        this.messageService = messageService;
        this.friendService = friendService;
        this.userRoles = userRoles;
    }

    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        AddFriendRequest request = WebUtils.readRequestBody2Obj(req, AddFriendRequest.class);
        friendService.addFriend(userRoles.getUid(), request.uid);
        return true;
    }

    public List<FriendService.FriendDto> doGet(HttpServletRequest req, HttpServletResponse resp) {
        return friendService.getMyFriends(userRoles.getUid());
    }



    @Data
    public static class AddFriendRequest {
        int uid;
    }
}
