package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

/**
 * 消息表
 */
@Data
@TableRegister("message")
public class Message {
    // 消息ID
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    int id;
    // 发送者ID
    @ColumnSetting(isNotNull = true)
    int senderId;
    // 如果是私聊，是接收者ID，如果是群聊，是0
    @ColumnSetting(isNotNull = true)
    int receiverId;
    // 如果是私聊，是0，如果是群聊，是群ID
    @ColumnSetting(isNotNull = true)
    int groupId;
    // 消息类型，普通文字消息为text，图片消息为image，文件消息为file，系统消息为system
    @ColumnSetting(stringLength = 20, isNotNull = true)
    String type;
    // 消息内容
    @ColumnSetting(stringLength = 1000, isNotNull = true)
    String content;
    // 消息回调地址，如果消息类型是system，这个字段是必须的，表示用户点击消息后请求的地址
    @ColumnSetting(stringLength = 100)
    String callback;
    // 消息状态，正常为normal，被撤回为withdraw，被删除为delete
    @ColumnSetting(stringLength = 20, isNotNull = true)
    String status;
    // 创建时间
    @ColumnSetting(isNotNull = true)
    int createTime;
}
