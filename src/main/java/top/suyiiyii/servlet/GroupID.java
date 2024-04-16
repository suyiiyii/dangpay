package top.suyiiyii.servlet;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.servlet.IngressServlet;

public class GroupID {
    private GroupService groupService;
    private IngressServlet.SubMethod subMethod;
    private RBACService rbacService;
    private UserRoles userRoles;

    public GroupID(GroupService groupService,
                   IngressServlet.SubMethod subMethod,
                   RBACService rbacService,
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
}
