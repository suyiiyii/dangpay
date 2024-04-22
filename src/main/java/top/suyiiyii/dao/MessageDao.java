package top.suyiiyii.dao;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Message;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.orm.core.Session;

public class MessageDao {
    Session db;
    RBACService rbacService;
    UserRoles userRoles;

    public MessageDao(Session db, @Proxy(isNeedAuthorization = false) RBACService rbacService, UserRoles userRoles) {
        this.db = db;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
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
}
