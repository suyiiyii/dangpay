package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.GroupServiceImpl;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.service.WalletService;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;
import top.suyiiyii.su.validator.Regex;

import java.util.List;

public class GroupID {
    private GroupService groupService;
    private IngressServlet.SubMethod subMethod;
    private RBACService rbacService;
    private UserRoles userRoles;
    private WalletService walletService;

    public GroupID(GroupService groupService,
                   IngressServlet.SubMethod subMethod,
                   @Proxy(isNeedAuthorization = false) RBACService rbacService,
                   WalletService walletService,
                   UserRoles userRoles) {
        this.groupService = groupService;
        this.subMethod = subMethod;
        this.rbacService = rbacService;
        this.walletService = walletService;
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
