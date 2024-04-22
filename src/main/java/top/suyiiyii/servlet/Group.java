package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.validator.Regex;

import java.util.List;

public class Group {
    private final GroupService groupService;
    private final UserRoles userRoles;
    private final RBACService rbacService;

    public Group(GroupService groupService,
                 UserRoles userRoles,
                 @Proxy(isNeedAuthorization = false) RBACService rbacService) {
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.rbacService = rbacService;
    }

    public GroupModel doPost(HttpServletRequest req, HttpServletResponse resp) {
        GroupDto groupDto = WebUtils.readRequestBody2Obj(req, GroupDto.class);
        GroupModel groupModel = new GroupModel();
        UniversalUtils.updateObj(groupModel, groupDto);
        return groupService.createGroup(userRoles, groupModel);
    }

    public List<GroupService.GroupDto> doGet(HttpServletRequest req, HttpServletResponse resp) {
        return groupService.getAllGroup(rbacService.isAdmin(userRoles));
    }

    @Data
    static class GroupDto {
        @Regex(".{3,100}")
        String name;
        @Regex(".{0,100}")
        String pepoleCount;
        @Regex(".{3,100}")
        String enterpriseScale;
        @Regex(".{3,100}")
        String industry;
        @Regex(".{3,100}")
        String address;
        @Regex("^1[3-9]\\d{9}$")
        String contact;
    }
}
