package top.suyiiyii.service;

import lombok.Data;
import top.suyiiyii.models.Message;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.validator.Regex;

import java.util.List;

@Proxy(isNeedAuthorization = true)
public interface MessageService {
    void sendGroupMessage(@SubRegion(areaPrefix = "g") int gid, int uid, String message);

    void sendUserMessage(int uid, int receiverId, String message);

    void sendSystemMessage(int receiverId, String message, String uuid);

    void sendSystemMessage2Role(String gid, String message, String callbackUuid);

    List<Message> getUserMessage(int uid, int senderId);

    List<Message> getGroupMessage(@SubRegion(areaPrefix = "g") int gid);

    @Data
    class MessageSendRequest {
        @Regex("[0-9]+")
        public int receiverId;
        @Regex("[0-9]+")
        public int groupId;
        @Regex("(?s).{1,888}")
        public String message;
    }
}
