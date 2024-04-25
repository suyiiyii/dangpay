package top.suyiiyii.service;


import top.suyiiyii.dao.MessageDao;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Message;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.orm.core.Session;

import java.util.Comparator;
import java.util.List;

@Repository
public class MessageServiceImpl implements MessageService {

    Session db;
    RBACService rbacService;
    UserRoles userRoles;
    MessageDao messageDao;
    ConfigManger configManger;

    public MessageServiceImpl(Session db,
                              @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService,
                              UserRoles userRoles,
                              MessageDao messageDao,
                              ConfigManger configManger) {
        this.db = db;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
        this.messageDao = messageDao;
        this.configManger = configManger;
    }

    /**
     * 向群组发送消息
     */
    @Override
    public void sendGroupMessage(@SubRegion(areaPrefix = "g") int gid, int uid, String message) {
        messageDao.sendTextMessage(uid, 0, gid, message);
    }

    /**
     * 向用户发送消息
     */
    @Override
    public void sendUserMessage(int uid, int receiverId, String message) {
        messageDao.sendTextMessage(uid, receiverId, 0, message);
    }

    @Override
    public void sendSystemMessage(int receiverId, String message, String uuid) {
        if (uuid == null) uuid = "";
        if (uuid.isEmpty()) {
            messageDao.sendSystemMessage(receiverId, message, uuid);
        } else {
            messageDao.sendSystemMessage(receiverId, message,  "/approve?uuid=" + uuid);
        }
    }

    @Override
    public List<Message> getUserMessage(int uid, int senderId) {
        List<Message> messages1 = db.query(Message.class).eq("receiver_id", uid).eq("sender_id", senderId).all();
        List<Message> messages2 = db.query(Message.class).eq("receiver_id", senderId).eq("sender_id", uid).all();
        messages1.addAll(messages2);
        // 按时间排序
        messages1.sort(Comparator.comparingInt(Message::getCreateTime));
        return messages1;
    }

    @Override
    public List<Message> getGroupMessage(int gid) {
        return db.query(Message.class).eq("receiver_id", 0).eq("group_id", gid).all();
    }

}