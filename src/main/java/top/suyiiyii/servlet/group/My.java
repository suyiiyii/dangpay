package top.suyiiyii.servlet.group;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.RBACAuthorization;

import java.util.List;

public class My {

    private final GroupService groupService;
    private final UserRoles userRoles;
    private final RBACService rbacService;

    public My(GroupService groupService,
              UserRoles userRoles,
              @RBACAuthorization(isNeedAuthorization = false) RBACService rbacService) {
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.rbacService = rbacService;
    }

    public List<GroupService.GroupDto> doGet(HttpServletRequest req, HttpServletResponse resp) {
        return groupService.getMyGroup(userRoles.getUid());
    }
}
