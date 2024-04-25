package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.models.Message;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.service.*;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;
import top.suyiiyii.su.validator.Regex;

import java.util.List;

public class GroupID {
    private final GroupService groupService;
    private final IngressServlet.SubMethod subMethod;
    private final RBACService rbacService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    private final MessageService messageService;

    public GroupID(GroupService groupService,
                   IngressServlet.SubMethod subMethod,
                   @Proxy(isNeedAuthorization = false) RBACService rbacService,
                   WalletService walletService,
                   MessageService messageService,
                   UserRoles userRoles) {
        this.groupService = groupService;
        this.subMethod = subMethod;
        this.rbacService = rbacService;
        this.walletService = walletService;
        this.messageService = messageService;
        this.userRoles = userRoles;
    }

    GroupService.GroupDto doGet() {
        return groupService.getGroup(subMethod.getId(), rbacService.isAdmin(userRoles));
    }

    boolean doPostBan() {
        groupService.banGroup(subMethod.getId());
        return true;
    }

    boolean doPostUnban() {
        groupService.unbanGroup(subMethod.getId());
        return true;
    }

    boolean doPostHide() {
        groupService.hideGroup(subMethod.getId());
        return true;
    }

    boolean doPostUnhide() {
        groupService.unhideGroup(subMethod.getId());
        return true;
    }
//
//    boolean doDeleteLeave() {
//        groupService.leaveGroup(subMethod.getId(), userRoles.getUid());
//        return true;
//    }
//
//    boolean doDeleteMember() {
//        groupService.deleteGroupMember(userRoles.getUid(), subMethod.getId());
//        return true;
//    }

    List<GroupServiceImpl.MemberDto> doGetMembers() {
        return groupService.getGroupMembers(subMethod.getId());
    }

    boolean doPostJoin() {
        groupService.joinGroup(subMethod.getId(), userRoles.getUid());
        return true;
    }

    boolean doPostLeave() {
        groupService.leaveGroup(subMethod.getId(), userRoles.getUid());
        return true;
    }

    GroupService.GroupDto doPatch(HttpServletRequest req, HttpServletResponse resp) {
        GroupService.GroupDto groupDto = WebUtils.readRequestBody2Obj(req, GroupService.GroupDto.class);
        GroupModel groupModel = new GroupModel();
        UniversalUtils.updateObj(groupModel, groupDto);
        groupModel.setId(subMethod.getId());
        groupService.updateGroup(subMethod.getId(), userRoles, groupModel);
        return groupDto;
    }

    boolean doPostInvite(HttpServletRequest req, HttpServletResponse resp) {
        UserRequest userRequest = WebUtils.readRequestBody2Obj(req, UserRequest.class);
        groupService.inviteUser(subMethod.getId(), userRequest.uid);
        return true;
    }

    boolean doPostAddAdmin(HttpServletRequest req, HttpServletResponse resp) {
        UserRequest userRequest = WebUtils.readRequestBody2Obj(req, UserRequest.class);
        groupService.addAdmin(subMethod.getId(), userRequest.uid);
        return true;
    }

    boolean doPostKick(HttpServletRequest req, HttpServletResponse resp) {
        UserRequest userRequest = WebUtils.readRequestBody2Obj(req, UserRequest.class);
        groupService.kickGroupMember(subMethod.getId(), userRequest.uid);
        return true;
    }

    // 钱包部分
    public boolean doPostWallet(HttpServletRequest req, HttpServletResponse resp) {
        walletService.createGroupWallet(subMethod.getId());
        return true;
    }

    public List<Wallet> doGetWallet(HttpServletRequest req, HttpServletResponse resp) {
        return walletService.getGroupWallets(subMethod.getId());
    }

    public List<Wallet> doGetSubWallet(HttpServletRequest req, HttpServletResponse resp) {
        return walletService.getGroupSubWallets(subMethod.getId());
    }

    public boolean doPostSubWallet(HttpServletRequest req, HttpServletResponse resp) {
        UserRequest userRequest = WebUtils.readRequestBody2Obj(req, UserRequest.class);
        walletService.createGroupSubWallet(subMethod.getId(), userRequest.uid);
        return true;
    }

    public boolean doPostAllocate(HttpServletRequest req, HttpServletResponse resp) {
        AllocateDto allocateDto = WebUtils.readRequestBody2Obj(req, AllocateDto.class);
        walletService.allocateGroupWallet(subMethod.getId(), allocateDto.id, allocateDto.amount);
        return true;
    }

    public boolean doPostTransferCreator(HttpServletRequest req, HttpServletResponse resp) {
        UserRequest userRequest = WebUtils.readRequestBody2Obj(req, UserRequest.class);
        groupService.transferGroupCreator(subMethod.getId(), userRequest.uid);
        return true;
    }

    public boolean doDelete(HttpServletRequest req, HttpServletResponse resp) {
        groupService.destroyGroup(subMethod.getId());
        return true;
    }

    public List<Message> doGetMessage(HttpServletRequest req, HttpServletResponse resp) {
        return messageService.getGroupMessage(subMethod.getId());
    }

    public boolean doPostMessage(HttpServletRequest req, HttpServletResponse resp) {
        MessageService.MessageSendRequest request = WebUtils.readRequestBody2Obj(req, MessageService.MessageSendRequest.class);
        messageService.sendGroupMessage(subMethod.getId(), userRoles.getUid(), request.message);
        return true;
    }

    @Data
    public static class UserRequest {
        @Regex("[0-9]+")
        private int uid;
    }

    @Data
    public static class AllocateDto {
        @Regex("[0-9]+")
        private int id;
        @Regex("-?[0-9]+")
        private int amount;
    }


}
