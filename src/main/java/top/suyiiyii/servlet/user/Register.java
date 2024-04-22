package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.models.User;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.validator.Regex;

import java.io.IOException;

import static top.suyiiyii.su.WebUtils.readRequestBody2Obj;
import static top.suyiiyii.su.WebUtils.respWrite;

public class Register {
    UserService userService;

    public Register(UserService userService) {
        super();
        this.userService = userService;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RegisterRequest registerRequest = readRequestBody2Obj(request, RegisterRequest.class);
        User user = userService.register(registerRequest.username, registerRequest.password, registerRequest.phone);
        respWrite(response, user);
    }

    static class RegisterRequest {
        @Regex("^[a-zA-Z0-9_-]{3,16}$")
        public String username;
        @Regex("^[a-zA-Z0-9_-]{6,18}$")
        public String password;
        @Regex("^1[3-9]\\d{9}$")
        public String phone;
    }
}


