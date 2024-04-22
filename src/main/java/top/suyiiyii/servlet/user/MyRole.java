package top.suyiiyii.servlet.user;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.Proxy;

public class MyRole {

    private final GroupService groupService;
    private final UserRoles userRoles;
    private final RBACService rbacService;

    public MyRole(GroupService groupService, UserRoles userRoles, @Proxy(isNeedAuthorization = false) RBACService rbacService) {
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.rbacService = rbacService;
    }

    public UserRoles doGet() {
        return userRoles;
    }
}
