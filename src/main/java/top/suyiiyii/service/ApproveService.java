package top.suyiiyii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Friend;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.models.PendingMethod;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.IOCManager;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ApproveService {
    private final Session db;
    private final RBACService rbacService;
    private final UserRoles userRoles;
    private final IOCManager iocManager;
    private final MessageService messageService;
    private final GroupService groupService;

    public ApproveService(Session db,
                          @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService,
                          UserRoles userRoles,
                          IOCManager iocManager,
                          @Proxy(isNeedAuthorization = false, isNotProxy = true) MessageService messageService,
                          @Proxy(isNeedAuthorization = false, isNotProxy = true) GroupService groupService) {
        this.db = db;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
        this.iocManager = iocManager;
        this.messageService = messageService;
        this.groupService = groupService;
    }


    /**
     * 检查是否需要审批
     * 入口方法，由ingressServlet调用
     */
    public boolean checkApprove(int uid, String reason, Method method, List<Object> args) {
        String methodStr = method.getDeclaringClass().getName() + "/" + method.getName();

        // 申请加入群组
        if (methodStr.equals("top.suyiiyii.service.GroupService/joinGroup")) {
            int gid = (int) args.get(0);
            // 参数校验
            // 检查是否存在群组
            if (!db.query(GroupModel.class).eq("id", gid).exists()) {
                throw new Http_400_BadRequestException("群组不存在");
            }
            // 检查是否已经加入
            if (rbacService.checkUserRole(userRoles, "GroupMember/g" + gid)) {
                throw new Http_400_BadRequestException("已经加入");
            }
            // 加群申请
            String uuid = submitApplicant(uid, reason, method, args);
            // 给管理员发送消息
            List<Integer> admins = rbacService.getUserByRole("GroupAdmin/g" + gid);
            String message = "用户" + uid + "申请加入群组" + gid + "，理由：" + reason;

            for (Integer admin : admins) {
                messageService.sendSystemMessage(admin, message, uuid);
            }
            return true;
        }
        // 邀请用户加入群组
        if (methodStr.equals("top.suyiiyii.service.GroupService/inviteUser")) {
            int gid = (int) args.get(0);
            int inviteUid = (int) args.get(1);
            // 参数校验
            // 检查是否存在群组
            if (!db.query(GroupModel.class).eq("id", gid).exists()) {
                throw new Http_400_BadRequestException("群组不存在");
            }
            // 检查是否已经加入
            if (rbacService.checkUserRole(userRoles, "GroupMember/g" + gid)) {
                throw new Http_400_BadRequestException("已经加入");
            }
            // 邀请申请
            String uuid = submitApplicant(inviteUid, reason, method, args);
            // 给被邀请者发送消息
            messageService.sendSystemMessage(inviteUid, "您被邀请加入群组" + gid + "，理由：" + reason, uuid);
            return true;
        }
        // 申请创建群组
        if (methodStr.equals("top.suyiiyii.service.GroupService/createGroup")) {
            // 参数校验
            //检查是否有同名群组
            String groupName = ((GroupModel) args.get(1)).getName();
            if (db.query(GroupModel.class).eq("name", groupName).exists()) {
                throw new Http_400_BadRequestException("已有同名群组");
            }
            // 创建群组申请
            String uuid = submitApplicant(uid, reason, method, args);
            // 给管理员发送消息
            List<Integer> admins = rbacService.getUserByRole("admin");
            String message = "用户" + uid + "申请创建群组，理由：" + reason;
            for (Integer admin : admins) {
                messageService.sendSystemMessage(admin, message, uuid);
            }
            return true;
        }
        // 好友申请
        if (methodStr.equals("top.suyiiyii.service.FriendService/addFriend")) {
            int uid1 = (int) args.get(0);
            int uid2 = (int) args.get(1);
            // 参数校验
            // 检查是否存在用户
            if (!db.query(User.class).eq("id", uid2).exists()) {
                throw new Http_400_BadRequestException("用户不存在");
            }
            // 检查是否添加自己
            if (uid1 == uid2) {
                throw new Http_400_BadRequestException("不能添加自己为好友");
            }
            // 检查是否已经是好友
            if (db.query(Friend.class).eq("uid1", uid1).eq("uid2", uid2).exists()) {
                throw new Http_400_BadRequestException("已经是好友");
            }
            // 好友申请
            String uuid = submitApplicant(uid, reason, method, args);
            // 给被邀请者发送消息
            messageService.sendSystemMessage(uid2, "用户" + uid + "申请添加您为好友，理由：" + reason, uuid);
            return true;
        }

        return false;
    }

    /**
     * 提交申请，由ingressServlet调用
     */
    @SneakyThrows
    public String submitApplicant(int uid, String reason, Method method, List<Object> args) {
        ObjectMapper objectMapper = new ObjectMapper();
        // 序列化方法名和参数，以字符串的形式存入数据库，以便日后反序列化继续执行操作
        String methodStr = method.getDeclaringClass().getName() + "/" + method.getName();
        String argsStr = objectMapper.writeValueAsString(args);

        // 检查是否存在用户
        if (!db.query(User.class).eq("id", uid).exists()) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        // 检查是否已经提交过
        if (db.query(PendingMethod.class).eq("applicant_id", uid).eq("method", methodStr).eq("args", argsStr).exists()) {
            throw new Http_400_BadRequestException("已经提交过");
        }
        // 提交申请
        PendingMethod pendingMethod = new PendingMethod();
        pendingMethod.setApplicantId(uid);
        pendingMethod.setMethod(methodStr);
        pendingMethod.setArgs(argsStr);
        pendingMethod.setReason(reason == null ? "" : reason);
        pendingMethod.setStatus("pending");
        pendingMethod.setCreateTime((int) (System.currentTimeMillis() / 1000));
        pendingMethod.setUuid(java.util.UUID.randomUUID().toString());
        db.insert(pendingMethod);
        return pendingMethod.getUuid();
    }

    @SneakyThrows
    public void approve(String uuid, String reason) {
        PendingMethod pendingMethod = db.query(PendingMethod.class).eq("uuid", uuid).first();
        if (pendingMethod == null) {
            throw new Http_400_BadRequestException("申请不存在");
        }
        if (!pendingMethod.getStatus().equals("pending")) {
            throw new Http_400_BadRequestException("申请已处理");
        }
        // 反序列化方法名和参数，继续执行操作
        ObjectMapper objectMapper = new ObjectMapper();
        String methodStr = pendingMethod.getMethod();
        String argsStr = pendingMethod.getArgs();
        String className = methodStr.split("/")[0];
        String methodName = methodStr.split("/")[1];
        List<Object> args = objectMapper.readValue(argsStr, List.class);
        // 反射执行方法
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, args.stream().map(Object::getClass).map(aClass -> {
            if (aClass == Integer.class) {
                return int.class;
            }
            return aClass;
        }).toArray(Class[]::new));
        try {
            method.invoke(iocManager.getObj(clazz, true, false), args.toArray());
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } finally {
            pendingMethod.setStatus("approved");
            db.commit();
        }
        // 给用户发送消息
        messageService.sendSystemMessage(pendingMethod.getApplicantId(), "申请通过，理由：" + reason, null);
    }

    @SneakyThrows
    public void reject(String uuid, String reason) {
        PendingMethod pendingMethod = db.query(PendingMethod.class).eq("uuid", uuid).first();
        if (pendingMethod == null) {
            throw new Http_400_BadRequestException("申请不存在");
        }
        if (!pendingMethod.getStatus().equals("pending")) {
            throw new Http_400_BadRequestException("申请已处理");
        }
        pendingMethod.setStatus("rejected");
        db.commit();
        // 给用户发送消息
        messageService.sendSystemMessage(pendingMethod.getApplicantId(), "申请被拒绝，理由：" + reason, null);
    }


    @Data
    public static class NeedApproveResponse {
        boolean needApprove;
        String msg;
    }

    @Data
    public static class ApplicantReason {
        String reason;
    }
}
