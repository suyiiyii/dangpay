package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.su.IOC.IOCManager;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.orm.core.ModelManger;

import java.io.IOException;

@WebServlet("/health")
public class healthCheck extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HealthCheckResponse response = new HealthCheckResponse();
        response.setPlatform(System.getenv("PLATFORM_NAME"));
        response.setServer(System.getenv("HOSTNAME"));
        if (response.getPlatform() == null) {
            response.setPlatform("dangpay local");
        }
        if (response.getServer() == null) {
            response.setServer("signleton");
        }
        WebUtils.respWrite(resp, response);

        int connectionCount = IOCManager.getBean(ModelManger.class).getConnectionManger().getLeaveCapacity();
        if (connectionCount < 20) {
            resp.setStatus(500);
            WebUtils.respWrite(resp, "数据库连接池不足");
        }
    }

    @Data
    public static class HealthCheckResponse {
        String platform;
        String server;
    }
}
