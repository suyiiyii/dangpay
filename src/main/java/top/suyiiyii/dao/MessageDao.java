package top.suyiiyii.dao;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Message;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.orm.core.Session;

public class MessageDao {
    Session db;
    UserRoles userRoles;
    ConfigManger configManger;

    public MessageDao(Session db, UserRoles userRoles,
                      ConfigManger configManger) {
        this.db = db;
        this.userRoles = userRoles;
        this.configManger = configManger;
    }

    public void sendTextMessage(int uid, int receiverId, int gid, String message) {
        Message msg = new Message();
        msg.setGroupId(gid);
        msg.setSenderId(uid);
        msg.setReceiverId(receiverId);
        msg.setType("text");
        msg.setContent(message);
        msg.setStatus("normal");
        msg.setCreateTime(UniversalUtils.getNow());
        db.insert(msg);
    }

    public void sendSystemMessage(int receiverId, String message, String callback) {
        Message msg = new Message();
        msg.setGroupId(0);
        msg.setSenderId(-1);
        msg.setReceiverId(receiverId);
        msg.setType("system");
        msg.setContent(message);
        msg.setStatus("normal");
        msg.setCreateTime(UniversalUtils.getNow());
        msg.setCallback(callback);
        db.insert(msg);
    }
}
