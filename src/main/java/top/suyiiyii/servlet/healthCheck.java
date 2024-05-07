package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.IOC.IOCManager;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.exception.Http_500_InternalServerErrorException;
import top.suyiiyii.su.orm.core.ModelManger;

import java.io.IOException;

@Slf4j
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
            log.error("数据库连接池不足");
            throw new Http_500_InternalServerErrorException("数据库连接池不足");
        }
        log.info("健康检查正常返回，数据库连接池剩余连接数：" + connectionCount);

    }

    @Data
    public static class HealthCheckResponse {
        String platform;
        String server;
    }
}
