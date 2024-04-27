package top.suyiiyii.service;

import lombok.Data;
import top.suyiiyii.su.IOC.Proxy;

import java.lang.reflect.Method;
import java.util.List;

@Proxy(isNeedAuthorization = true)
public interface ApproveService {
    boolean checkApprove(int uid, String reason, Method method, List<Object> args);

    String submitApplicant(int uid, String reason, Method method, List<Object> args);

    void approve(String uuid, String reason);

    void reject(String uuid, String reason);

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
