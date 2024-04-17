package top.suyiiyii.service;

import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.su.IOC.RBACAuthorization;
import top.suyiiyii.su.IOC.SubRegion;

import java.util.List;

@RBACAuthorization
public interface GroupService {
    GroupModel createGroup(UserRoles userRoles, GroupModel groupModel);


    void updateGroup(@SubRegion(areaPrefix = "g") int gid, UserRoles userRoles, GroupModel groupModel);

    List<GroupModel> getAllGroup(boolean isSeeBan);

    List<GroupModel> getAllGroup();


    List<GroupDto> getMyGroup(int uid);

    GroupModel getGroup(int gid, boolean isSeeBan);

    GroupModel getGroup(int gid);


    void banGroup(@SubRegion(areaPrefix = "g") int gid);


    void unbanGroup(@SubRegion(areaPrefix = "g") int gid);


    void hideGroup(@SubRegion(areaPrefix = "g") int gid);


    void unhideGroup(@SubRegion(areaPrefix = "g") int gid);

    void joinGroup(int gid, int uid);


    void leaveGroup(@SubRegion(areaPrefix = "g") int gid, int uid);


    void deleteGroupMember(@SubRegion(areaPrefix = "g") int gid, int uid);


    List<MemberDto> getGroupMembers(@SubRegion(areaPrefix = "g") int gid);

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
