package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupMember;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;
import java.util.NoSuchElementException;

public class GroupService {
    Session db;

    public GroupService(Session db) {
        this.db = db;
    }

    public GroupModel createGroup(GroupModel groupModel, UserRoles userRoles) {
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

    public List<GroupModel> getAllGroup(boolean isSeeBan) {
        List<GroupModel> groupModels = db.query(GroupModel.class).all();
        if (!isSeeBan) {
            groupModels.removeIf(groupModel -> groupModel.getStatus().equals("ban"));
            groupModels.removeIf(groupModel -> groupModel.getHide().equals("true"));
        }
        return groupModels;
    }

    public List<GroupModel> getAllGroup() {
        return getAllGroup(false);
    }

    public List<GroupModel> getMyGroup(int uid) {
        // 先获取用户的所有群组
        List<GroupMember> groupMembers = db.query(GroupMember.class).eq("user_id", uid).all();
        // 再获取群组的详细信息
        List<GroupModel> groupModels = db.query(GroupModel.class).in("id", List.of(groupMembers.stream().map(GroupMember::getGroupId).toArray())).all();
        return groupModels;
    }


    public GroupModel getGroup(int id, boolean isSeeBan) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", id).first();
        if (groupModel.getStatus().equals("ban") && !isSeeBan) {
            throw new NoSuchElementException("群组不存在");
        }
        return groupModel;
    }

    public GroupModel getGroup(int id) {
        return getGroup(id, false);
    }

    public void banGroup(int id) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", id).first();
        groupModel.setStatus("ban");
        db.commit();
    }

    public void unbanGroup(int id) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", id).first();
        groupModel.setStatus("normal");
        db.commit();
    }

    public void hideGroup(int id) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", id).first();
        groupModel.setHide("true");
        db.commit();
    }

    public void unhideGroup(int id) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", id).first();
        groupModel.setHide("false");
        db.commit();
    }
}
