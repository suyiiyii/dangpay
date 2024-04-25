package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.models.User;
import top.suyiiyii.service.MailService;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.validator.Regex;

import java.io.IOException;

import static top.suyiiyii.su.WebUtils.readRequestBody2Obj;
import static top.suyiiyii.su.WebUtils.respWrite;

public class Register {
    UserService userService;
    MailService mailService;

    public Register(UserService userService,
                    MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RegisterRequest registerRequest = readRequestBody2Obj(request, RegisterRequest.class);
        if (!mailService.verifyCode(registerRequest.email, registerRequest.verifyCode)) {
            throw new Http_400_BadRequestException("验证码错误");
        }
        User user = userService.register(registerRequest.username, registerRequest.password, registerRequest.phone, registerRequest.email);
        respWrite(response, user);
    }

    static class RegisterRequest {
        @Regex("^[a-zA-Z0-9_-]{3,16}$")
        public String username;
        @Regex("^[a-zA-Z0-9_-]{6,18}$")
        public String password;
        @Regex("^1[3-9]\\d{9}$")
        public String phone;
        @Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        public String email;
        @Regex("^[a-zA-Z0-9_-]{4}$")
        public String verifyCode;
    }
}


