package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;

public class Group {
    private final GroupService groupService;

    public Group(GroupService groupService) {
        this.groupService = groupService;
    }

    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        GroupDto groupDto = WebUtils.readRequestBody2Obj(req, GroupDto.class);
        GroupModel groupModel = new GroupModel();
        UniversalUtils.updateObj(groupModel, groupDto);
        groupService.addGroup(groupModel);
        return true;
    }

    @Data
    static class GroupDto {
        String name;
        String pepoleCount;
        String enterpriseScale;
        String industry;
        String address;
        String contact;
    }
}
