package top.suyiiyii.service;

import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.List;

public interface ApproveService {
    boolean checkApprove(int uid, String reason, Method method, List<Object> args);

    @SneakyThrows
    String submitApplicant(int uid, String reason, Method method, List<Object> args);

    @SneakyThrows
    void approve(String uuid, String reason);

    @SneakyThrows
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
