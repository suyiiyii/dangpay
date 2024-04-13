package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.BaseHttpServlet;

import java.io.IOException;

@WebServlet(urlPatterns = "/health")
public class Health_check extends BaseHttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebUtils.respWrite(resp, "ok");
    }
}
