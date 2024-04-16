package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.models.User;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.servlet.IngressServlet;

import java.io.IOException;

public class UserID {
    private UserService userService;
    private IngressServlet.SubMethod subMethod;

    public UserID(UserService userService,
                  IngressServlet.SubMethod subMethod) {
        this.userService = userService;
        this.subMethod = subMethod;
    }

    protected User doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        return userService.getUser(subMethod.getId());
    }

    protected boolean doPostBan(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        userService.banUser(subMethod.getId());
        return true;
    }

    protected boolean doPostUnban(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        userService.unbanuser(subMethod.getId());
        return true;
    }
}
