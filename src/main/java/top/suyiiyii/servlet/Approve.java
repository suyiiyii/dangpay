package top.suyiiyii.servlet;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.service.ApproveService;
import top.suyiiyii.su.WebUtils;

public class Approve {

    ApproveService approveService;

    public Approve(ApproveService approveService) {
        this.approveService = approveService;
    }

    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        String uuid = req.getParameter("uuid");
        approveRequest approveRequest = WebUtils.readRequestBody2Obj(req, approveRequest.class);
        if (approveRequest.isApprove) {
            approveService.approve(uuid, approveRequest.reason);
            return true;
        } else {
            approveService.reject(uuid, approveRequest.reason);
            return false;
        }
    }

    @Data
    public static class approveRequest {
        @JsonProperty("isApprove")
        boolean isApprove;
        String reason;
    }
}
