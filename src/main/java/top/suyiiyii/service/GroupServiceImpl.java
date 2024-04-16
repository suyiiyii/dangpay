package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupMember;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.RBACAuthorization;
import top.suyiiyii.su.IOC.Repository;
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

    public GroupServiceImpl(Session db) {
        this.db = db;
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
            GroupMember groupMember = new GroupMember();
            groupMember.setGroupId(groupModel.getId());
            groupMember.setUserId(userRoles.getUid());
            groupMember.setRole("admin");
            db.insert(groupMember);
            db.commitTransaction();
            return groupModel;
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 更新群组信息
     * //TODO
     */
    @Override
    public void updateGroup(UserRoles userRoles, GroupModel groupModel) {
        // 判断是否是管理员
        if (!db.query(GroupMember.class).eq("user_id", userRoles.getUid()).eq("group_id", groupModel.getId()).eq("role", "admin").exists()) {
            throw new Http_400_BadRequestException("你不是管理员");
        }


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
        List<GroupMember> groupMembers = db.query(GroupMember.class).eq("user_id", uid).all();
        // 再获取群组的详细信息
        List<GroupModel> groupModels = db.query(GroupModel.class).in("id", List.of(groupMembers.stream().map(GroupMember::getGroupId).toArray())).all();
        // 封装数据
        List<GroupDto> groupDtos = groupModels.stream().map(groupModel -> {
            GroupDto groupDto = new GroupDto();
            UniversalUtils.updateObj(groupDto, groupModel);
            groupDto.amIAdmin = groupMembers.stream().anyMatch(groupMember -> groupMember.getGroupId() == groupModel.getId() && groupMember.getRole().equals("admin"));
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
    public GroupModel getGroup(int gid, boolean isSeeBan) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        if (groupModel.getStatus().equals("ban") && !isSeeBan) {
            throw new NoSuchElementException("群组不存在");
        }
        return groupModel;
    }

    @Override
    public GroupModel getGroup(int gid) {
        return getGroup(gid, false);
    }

    /**
     * 封禁一个群组
     *
     * @param gid 群组id
     */
    @Override
    public void banGroup(int gid) {
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
    public void unbanGroup(int gid) {
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
    public void hideGroup(int gid) {
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
    public void unhideGroup(int gid) {
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
    public void joinGroup(int gid, int uid) {
        try {
            db.beginTransaction();
            GroupMember groupMember = new GroupMember();
            groupMember.setUserId(uid);
            groupMember.setGroupId(gid);
            groupMember.setRole("member");
            db.insert(groupMember);
            db.commitTransaction();
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
    @RBACAuthorization(subId = "gid")
    public void leaveGroup(int gid, int uid) {
        try {
            db.beginTransaction();
            db.delete(GroupMember.class).eq("user_id", uid).eq("group_id", gid).all();
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
    @RBACAuthorization(subId = "gid")
    public void deleteGroupMember(int gid, int uid) {
        try {
            db.beginTransaction();
            db.delete(GroupMember.class).eq("user_id", uid).eq("group_id", gid).all();
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
    @RBACAuthorization(subId = "gid")
    public List<MemberDto> getGroupMembers(int gid) {
        // 先获取群组的所有成员id
        List<GroupMember> groupMembers = db.query(GroupMember.class).eq("group_id", gid).all();
        // 再获取成员的名字
        List<User> users = db.query(User.class).in("id", List.of(groupMembers.stream().map(GroupMember::getUserId).toArray())).all();
        Map<Integer, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        List<MemberDto> memberDtos = groupMembers.stream().map(groupMember -> {
            MemberDto memberDto = new MemberDto();
            memberDto.setId(groupMember.getUserId());
            memberDto.setName(userMap.get(groupMember.getUserId()));
            memberDto.setRole(groupMember.getRole());
            return memberDto;
        }).toList();
        return memberDtos;
    }

}
