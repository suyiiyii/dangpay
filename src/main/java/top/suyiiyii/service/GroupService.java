package top.suyiiyii.service;

import top.suyiiyii.models.GroupModel;
import top.suyiiyii.su.orm.core.Session;

public class GroupService {
    Session db;

    public GroupService(Session db) {
        this.db = db;
    }

    public GroupModel addGroup(GroupModel groupModel) {
        db.insert(groupModel);
        groupModel = db.query(GroupModel.class).eq("name", groupModel.getName()).first();
        return groupModel;
    }
}
