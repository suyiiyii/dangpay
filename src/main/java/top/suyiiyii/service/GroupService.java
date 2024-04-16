package top.suyiiyii.service;

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

    public GroupModel addGroup(GroupModel groupModel) {
        try {
            db.query(GroupModel.class).eq("name", groupModel.getName()).first();
            throw new Http_400_BadRequestException("群组名已存在");
        } catch (NoSuchElementException ignored) {
        }
        groupModel.setStatus("normal");
        db.insert(groupModel);
        groupModel = db.query(GroupModel.class).eq("name", groupModel.getName()).first();
        return groupModel;
    }

    public List<GroupModel> getAllGroup(boolean isSeeBan) {
        List<GroupModel> groupModels = db.query(GroupModel.class).all();
        if (!isSeeBan) {
            groupModels.removeIf(groupModel -> groupModel.getStatus().equals("ban"));
        }
        return groupModels;
    }

    public List<GroupModel> getAllGroup() {
        return getAllGroup(false);
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
}
