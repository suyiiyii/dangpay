package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Message;
import top.suyiiyii.service.*;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;

import java.util.List;

public class FriendID {

    private final GroupService groupService;
    private final IngressServlet.SubMethod subMethod;
    private final RBACService rbacService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    private final MessageService messageService;
    private final FriendService friendService;

    public FriendID(GroupService groupService,
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


    public boolean doDelete(HttpServletRequest req, HttpServletResponse resp) {
        friendService.deleteFriend(userRoles.getUid(), subMethod.getId());
        return true;
    }

    public boolean doPostMessage(HttpServletRequest req, HttpServletResponse resp) {
        MessageService.MessageSendRequest request = WebUtils.readRequestBody2Obj(req, MessageService.MessageSendRequest.class);
        messageService.sendUserMessage(userRoles.getUid(), subMethod.getId(), request.message);
        return true;
    }

    public List<Message> doGetMessage(HttpServletRequest req, HttpServletResponse resp) {
        return messageService.getUserMessage(userRoles.getUid(), subMethod.getId());
    }
}
