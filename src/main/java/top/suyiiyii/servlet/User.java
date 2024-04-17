package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.UserService;

import java.io.IOException;

public class User {
    private final UserService userService;
    private final UserRoles userRoles;

    public User(UserService userService,
                UserRoles userRoles) {
        this.userService = userService;
        this.userRoles = userRoles;
    }

    /**
     * 获取用户信息接口
     *
     * @param req  an {@link HttpServletRequest} object that contains the request the client has made of the servlet
     * @param resp an {@link HttpServletResponse} object that contains the response the servlet sends to the client
     * @throws ServletException
     * @throws IOException
     */
    protected Object doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        return userService.getUsers(userRoles);
    }

}
