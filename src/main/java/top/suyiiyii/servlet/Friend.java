package top.suyiiyii.servlet;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.*;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;

import java.util.List;

public class Friend {
    private final GroupService groupService;
    private final IngressServlet.SubMethod subMethod;
    private final RBACService rbacService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    private final MessageService messageService;
    private final FriendService friendService;

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

    public List<FriendServiceImpl.FriendDto> doGet(HttpServletRequest req, HttpServletResponse resp) {
        return friendService.getMyFriends(userRoles.getUid());
    }


    @Data
    public static class AddFriendRequest {
        int uid;
    }
}
