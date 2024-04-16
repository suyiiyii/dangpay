package top.suyiiyii.service;

import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.su.IOC.RBACAuthorization;

import java.util.List;

@RBACAuthorization
public interface GroupService {
    GroupModel createGroup(UserRoles userRoles, GroupModel groupModel);

    @RBACAuthorization(subId = "gid")
    void updateGroup(int gid, UserRoles userRoles, GroupModel groupModel);

    List<GroupModel> getAllGroup(boolean isSeeBan);

    List<GroupModel> getAllGroup();


    List<GroupDto> getMyGroup(int uid);

    GroupModel getGroup(int gid, boolean isSeeBan);

    GroupModel getGroup(int gid);

    @RBACAuthorization(subId = "gid")
    void banGroup(int gid);

    @RBACAuthorization(subId = "gid")
    void unbanGroup(int gid);

    @RBACAuthorization(subId = "gid")
    void hideGroup(int gid);

    @RBACAuthorization(subId = "gid")
    void unhideGroup(int gid);

    void joinGroup(int gid, int uid);

    @RBACAuthorization(subId = "gid")
    void leaveGroup(int gid, int uid);

    @RBACAuthorization(subId = "gid")
    void deleteGroupMember(int gid, int uid);

    @RBACAuthorization(subId = "gid")
    List<MemberDto> getGroupMembers(int gid);

    @Data
    public static class GroupDto {
        int id;
        String name;
        String pepoleCount;
        String enterpriseScale;
        String industry;
        String address;
        String contact;
        String status;
        String hide;
        boolean amIAdmin;
    }

    @Data
    public static class MemberDto {
        int id;
        String name;
        String role;
    }
}
