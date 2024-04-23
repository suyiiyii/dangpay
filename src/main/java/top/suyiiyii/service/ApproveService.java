package top.suyiiyii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.PendingMethod;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.IOCmanager;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.lang.reflect.Method;
import java.util.List;

public class ApproveService {
    Session db;
    RBACService rbacService;
    UserRoles userRoles;
    IOCmanager iocManager;

    public ApproveService(Session db, @Proxy(isNeedAuthorization = false) RBACService rbacService, UserRoles userRoles,
                          IOCmanager iocManager) {
        this.db = db;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
        this.iocManager = iocManager;
    }

    /**
     * 提交申请，由ingressServlet调用
     */
    @SneakyThrows
    public boolean submitApprove(int uid, String reason, Method method, List<Object> args) {
        ObjectMapper objectMapper = new ObjectMapper();
        // 序列化方法名和参数，以字符串的形式存入数据库，以便日后反序列化继续执行操作
        String methodStr = method.getDeclaringClass().getName() + "/" + method.getName();
        String argsStr = objectMapper.writeValueAsString(args);

        // 检查是否存在用户
        if (!db.query(User.class).eq("id", uid).exists()) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        // 检查是否已经提交过
        if (db.query(PendingMethod.class).eq("applicantId", uid).eq("method", method).eq("args", args).exists()) {
            throw new Http_400_BadRequestException("已经提交过");
        }
        // 提交申请
        PendingMethod pendingMethod = new PendingMethod();
        pendingMethod.setApplicantId(uid);
        pendingMethod.setMethod(methodStr);
        pendingMethod.setArgs(argsStr);
        pendingMethod.setReason(reason);
        pendingMethod.setStatus("pending");
        pendingMethod.setCreateTime((int) (System.currentTimeMillis() / 1000));
        db.insert(pendingMethod);
        return true;
    }

    @SneakyThrows
    public void approve(int id, boolean isApprove, String reason) {
        PendingMethod pendingMethod = db.query(PendingMethod.class).eq("id", id).first();
        if (pendingMethod == null) {
            throw new Http_400_BadRequestException("申请不存在");
        }
        if (!pendingMethod.getStatus().equals("pending")) {
            throw new Http_400_BadRequestException("申请已处理");
        }
        if (isApprove) {
            // 反序列化方法名和参数，继续执行操作
            ObjectMapper objectMapper = new ObjectMapper();
            String methodStr = pendingMethod.getMethod();
            String argsStr = pendingMethod.getArgs();
            String className = methodStr.split("/")[0];
            String methodName = methodStr.split("/")[1];
            List<Object> args = objectMapper.readValue(argsStr, List.class);
            // 反射执行方法
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, args.stream().map(Object::getClass).toArray(Class[]::new));
            method.invoke(iocManager.getObj(clazz), args.toArray());
        }
    }
}
