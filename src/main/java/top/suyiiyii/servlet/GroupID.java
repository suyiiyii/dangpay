package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.GroupServiceImpl;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.RBACAuthorization;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;

import java.util.List;

public class GroupID {
    private GroupService groupService;
    private IngressServlet.SubMethod subMethod;
    private RBACService rbacService;
    private UserRoles userRoles;

    public GroupID(GroupService groupService,
                   IngressServlet.SubMethod subMethod,
                   @RBACAuthorization(isNeedAuthorization = false) RBACService rbacService,
                   UserRoles userRoles) {
        this.groupService = groupService;
        this.subMethod = subMethod;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
    }

    GroupModel doGet() {
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

}
