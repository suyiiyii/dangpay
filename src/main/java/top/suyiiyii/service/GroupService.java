package top.suyiiyii.service;

import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.validator.Regex;

import java.util.List;

@Proxy
public interface GroupService {
    GroupModel createGroup(int uid, GroupModel groupModel);

    void updateGroup(@SubRegion(areaPrefix = "g") int gid, UserRoles userRoles, GroupModel groupModel);

    List<GroupDto> getAllGroup(boolean isSeeBan);

    List<GroupDto> getAllGroup();


    List<GroupDto> getMyGroup(int uid);

    GroupDto getGroup(int gid, boolean isSeeBan);

    GroupDto getGroup(int gid);


    void banGroup(@SubRegion(areaPrefix = "g") int gid);


    void unbanGroup(@SubRegion(areaPrefix = "g") int gid);


    void hideGroup(@SubRegion(areaPrefix = "g") int gid);


    void unhideGroup(@SubRegion(areaPrefix = "g") int gid);

    void joinGroup(int gid, int uid);


    void leaveGroup(@SubRegion(areaPrefix = "g") int gid, int uid);


    void kickGroupMember(@SubRegion(areaPrefix = "g") int gid, int uid);


    List<MemberDto> getGroupMembers(@SubRegion(areaPrefix = "g") int gid);

    void inviteUser(@SubRegion(areaPrefix = "g") int gid, int uid);

    void addAdmin(@SubRegion(areaPrefix = "g") int gid, int uid);

    void transferGroupCreator(@SubRegion(areaPrefix = "g") int gid, int uid);

    @Proxy(isTransaction = true)
    void destroyGroup(@SubRegion(areaPrefix = "g") int gid);

    void checkGroupStatus(int gid);

    @Data
    class GroupDto {
        int id;
        @Regex(".{3,20}")
        String name;
        @Regex(".{0,20}")
        String pepoleCount;
        @Regex(".{3,20}")
        String enterpriseScale;
        @Regex(".{3,20}")
        String industry;
        @Regex(".{3,20}")
        String address;
        @Regex("^1[3-9]\\d{9}$")
        String contact;
        String status;
        String hide;
        boolean amIAdmin;
        int groupCreatorId;
    }

    @Data
    class MemberDto {
        int id;
        String name;
        String role;
    }
}
