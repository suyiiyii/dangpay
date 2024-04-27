package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.User;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.IngressServlet;
import top.suyiiyii.su.validator.Regex;

import java.io.IOException;

public class UserID {
    private final UserService userService;
    private final UserRoles userRoles;
    private final IngressServlet.SubMethod subMethod;

    public UserID(UserService userService,
                  UserRoles userRoles,
                  IngressServlet.SubMethod subMethod) {
        this.userService = userService;
        this.userRoles = userRoles;
        this.subMethod = subMethod;
    }

    protected User doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        return userService.getUser(subMethod.getId(), userRoles);
    }

    protected boolean doPostBan(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        userService.banUser(subMethod.getId());
        return true;
    }

    protected boolean doPostUnban(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        userService.unbanuser(subMethod.getId());
        return true;
    }

    protected User doPatch(HttpServletRequest req, HttpServletResponse resp) {
        User user = WebUtils.readRequestBody2Obj(req, User.class);
        user.setId(subMethod.getId());
        return userService.updateUser(user, userRoles);
    }

    protected User doPostChangePassword(HttpServletRequest req, HttpServletResponse resp) {
        changePasswordRequest request = WebUtils.readRequestBody2Obj(req, changePasswordRequest.class);
        userService.changePassword(userRoles, subMethod.getId(), request.oldPassword, request.newPassword);
        return userService.getUser(subMethod.getId(), userRoles);
    }


    public static class changePasswordRequest {
        public String oldPassword;
        @Regex("^[a-zA-Z0-9_-]{6,18}$")
        public String newPassword;
    }
}
