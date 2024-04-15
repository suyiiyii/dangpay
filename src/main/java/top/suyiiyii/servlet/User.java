package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.orm.core.Session;
import top.suyiiyii.su.servlet.BaseHttpServlet;

import java.io.IOException;

public class User extends BaseHttpServlet {
    private Session db;
    private UserRoles userRoles;

    public User(Session db,
                UserRoles userRoles) {
        this.db = db;
        this.userRoles = userRoles;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        WebUtils.respWrite(resp, "你好" + userRoles.uid + "\n" + "你的身份是：" + userRoles.roles);
    }

}
