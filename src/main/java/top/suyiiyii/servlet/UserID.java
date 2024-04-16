package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.orm.core.Session;
import top.suyiiyii.su.servlet.IngressServlet;

import java.io.IOException;

public class UserID {
    private final Session db;
    private IngressServlet.SubMethod subMethod;

    public UserID(Session db,
                  IngressServlet.SubMethod subMethod) {
        this.db = db;
        this.subMethod = subMethod;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebUtils.respWrite(resp, "你好" + subMethod.getId() + "\n" + "你调用的是：" + subMethod.getName());
    }

    protected void doGetBan(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebUtils.respWrite(resp, "你好" + subMethod.getId() + "\n" + "你调用的是：" + subMethod.getName());
    }
}
