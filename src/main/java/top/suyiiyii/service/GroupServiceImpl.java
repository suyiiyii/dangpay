package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.models.RBACUser;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.RBACAuthorization;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Repository
public class GroupServiceImpl implements GroupService {
    Session db;
    RBACService rbacService;

    public GroupServiceImpl(Session db,
                            @RBACAuthorization(isNeedAuthorization = false) RBACService rbacService) {
        this.db = db;
        this.rbacService = rbacService;
    }

    /**
     * 创建群组
     *
     * @param userRoles  用户角色
     * @param groupModel 群组信息
     * @return 群组信息
     */
    @Override
    public GroupModel createGroup(UserRoles userRoles, GroupModel groupModel) {
        try {
            db.query(GroupModel.class).eq("name", groupModel.getName()).first();
            throw new Http_400_BadRequestException("群组名已存在");
        } catch (NoSuchElementException ignored) {
        }
        try {
            db.beginTransaction();
            // 创建群组
            groupModel.setStatus("normal");
            groupModel.setHide("false");
            db.insert(groupModel);
            groupModel = db.query(GroupModel.class).eq("name", groupModel.getName()).first();
            // 添加群组管理员
            rbacService.addUserRole(userRoles.getUid(), "GroupAdmin/g" + groupModel.getId());
            rbacService.addUserRole(userRoles.getUid(), "GroupMember/g" + groupModel.getId());
            db.commitTransaction();
            return groupModel;
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 更新群组信息
     */
    @Override
    public void updateGroup(@SubRegion(areaPrefix = "g") int gid, UserRoles userRoles, GroupModel groupModel) {
        GroupModel groupModel1 = db.query(GroupModel.class).eq("id", gid).first();
        if (!groupModel1.getName().equals(groupModel.getName())) {
            try {
                db.query(GroupModel.class).eq("name", groupModel.getName()).first();
                throw new Http_400_BadRequestException("群组名已存在");
            } catch (NoSuchElementException ignored) {
            }
        }
        UniversalUtils.updateObj(groupModel1, groupModel);
        db.commit();
    }

    /**
     * 获取所有群组
     * 用户默认不可见ban状态和隐藏的群组的群组
     *
     * @param isSeeBan 是否可见ban状态的群组
     * @return 群组列表
     */
    @Override
    public List<GroupModel> getAllGroup(boolean isSeeBan) {
        List<GroupModel> groupModels = db.query(GroupModel.class).all();
        if (!isSeeBan) {
            groupModels.removeIf(groupModel -> groupModel.getStatus().equals("ban"));
            groupModels.removeIf(groupModel -> groupModel.getHide().equals("true"));
        }
        return groupModels;
    }

    @Override
    public List<GroupModel> getAllGroup() {
        return getAllGroup(false);
    }

    /**
     * 获取用户的所有群组
     *
     * @param uid 用户id
     * @return 群组列表
     */


    @Override
    public List<GroupDto> getMyGroup(int uid) {
        // 先获取用户的所有群组
        List<RBACUser> rbacUsers = db.query(RBACUser.class).eq("uid", uid).fuzzLike("role", "GroupMember/g").all();
        // 再获取群组的详细信息
        List<GroupModel> groupModels = db.query(GroupModel.class).in("id", List.of(rbacUsers.stream().map(rbacUser -> Integer.parseInt(rbacUser.getRole().split("/")[1].substring(1))).toArray())).all();
        // 封装数据
        List<GroupDto> groupDtos = groupModels.stream().map(groupModel -> {
            GroupDto groupDto = new GroupDto();
            UniversalUtils.updateObj(groupDto, groupModel);
//            groupDto.setPepoleCount(String.valueOf(rbacUsers.stream().filter(rbacUser -> rbacUser.getRole().equals("GroupMember/" + groupModel.getId())).count()));
            groupDto.setAmIAdmin(rbacUsers.stream().anyMatch(rbacUser -> rbacUser.getRole().equals("GroupAdmin/g" + groupModel.getId())));
            return groupDto;
        }).toList();

        return groupDtos;
    }

    /**
     * 获取群组信息
     * 普通用户默认不可见ban状态的群组
     *
     * @param gid      群组id
     * @param isSeeBan 是否可见ban状态的群组
     * @return 群组信息
     */

    @Override
    public GroupModel getGroup(@SubRegion(areaPrefix = "g") int gid, boolean isSeeBan) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        if (groupModel.getStatus().equals("ban") && !isSeeBan) {
            throw new NoSuchElementException("群组不存在");
        }
        return groupModel;
    }

    @Override
    public GroupModel getGroup(@SubRegion(areaPrefix = "g") int gid) {
        return getGroup(gid, false);
    }

    /**
     * 封禁一个群组
     *
     * @param gid 群组id
     */
    @Override
    public void banGroup(@SubRegion(areaPrefix = "g") int gid) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setStatus("ban");
        db.commit();
    }

    /**
     * 解封一个群组
     *
     * @param gid 群组id
     */
    @Override
    public void unbanGroup(@SubRegion(areaPrefix = "g") int gid) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setStatus("normal");
        db.commit();
    }

    /**
     * 设置一个群组为隐藏状态
     *
     * @param gid 群组id
     */

    @Override
    public void hideGroup(@SubRegion(areaPrefix = "g") int gid) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setHide("true");
        db.commit();
    }

    /**
     * 设置一个群组为显示状态
     *
     * @param gid 群组id
     */

    @Override
    public void unhideGroup(@SubRegion(areaPrefix = "g") int gid) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setHide("false");
        db.commit();
    }

    /**
     * 删除一个群组成员
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void joinGroup(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            rbacService.addUserRole(uid, "GroupMember/g" + gid);
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 用户自己主动退出群组
     *
     * @param gid
     * @param uid
     */
    @Override
    public void leaveGroup(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            rbacService.deleteUserRole(uid, "GroupMember/g" + gid);
            rbacService.deleteUserRole(uid, "GroupAdmin/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 删除一个群组成员
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void kickGroupMember(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            rbacService.deleteUserRole(uid, "GroupMember/g" + gid);
            rbacService.deleteUserRole(uid, "GroupAdmin/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 获取群组的所有成员
     *
     * @param gid 群组id
     * @return 成员列表
     */
    @Override
    public List<MemberDto> getGroupMembers(@SubRegion(areaPrefix = "g") int gid) {
        // 先获取群组的所有成员id
        List<RBACUser> rbacUsers = db.query(RBACUser.class).eq("role", "GroupMember/g" + gid).all();
        // 获取管理员的id
        List<RBACUser> rbacAdmins = db.query(RBACUser.class).eq("role", "GroupAdmin/g" + gid).all();
        // 再获取成员的名字
        List<User> users = db.query(User.class).in("id", List.of(rbacUsers.stream().map(RBACUser::getUid).toArray())).all();
        Map<Integer, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        //构造返回数据
        List<MemberDto> memberDtos = rbacUsers.stream().map(rbacUser -> {
            MemberDto memberDto = new MemberDto();
            memberDto.setId(rbacUser.getUid());
            memberDto.setName(userMap.get(rbacUser.getUid()));
            memberDto.setRole(rbacAdmins.stream().anyMatch(rbacUser1 -> rbacUser1.getUid() == rbacUser.getUid()) ? "admin" : "member");
            return memberDto;
        }).toList();
        return memberDtos;
    }

    /**
     * 邀请用户加入群组
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void inviteUser(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            rbacService.addUserRole(uid, "GroupMember/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    @Override
    public void addAdmin(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            rbacService.addUserRole(uid, "GroupAdmin/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }


}
