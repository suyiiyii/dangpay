package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.models.User;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.servlet.BaseHttpServlet;

import java.io.IOException;

import static top.suyiiyii.su.WebUtils.readRequestBody2Obj;
import static top.suyiiyii.su.WebUtils.respWrite;

public class Register extends BaseHttpServlet {
    UserService userService;

    public Register(UserService userService) {
        this.userService = userService;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RegisterRequest registerRequest = readRequestBody2Obj(request, RegisterRequest.class);
        User user = userService.register(registerRequest.username, registerRequest.password, registerRequest.phone);
        respWrite(response, user);
    }
}


class RegisterRequest {
    public String username;
    public String password;
    public String phone;
}