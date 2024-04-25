package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.Token;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.service.CaptchaService;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.validator.Regex;
import top.suyiiyii.su.validator.Validator;

import java.io.IOException;

@Slf4j
public class Login {
    UserService userService;
    CaptchaService captchaService

    public Login(UserService userService,
                 CaptchaService captchaService) {
        this.userService = userService;
        this.captchaService = captchaService;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        LoginRequest request = new LoginRequest();

        request.grant_type = req.getParameter("grant_type");
        request.username = req.getParameter("username");
        request.password = req.getParameter("password");
        request.captcha = req.getParameter("captcha");
        Validator.check(request);

        log.info("校验验证码：" + request.captcha);
        // 验证码校验
        if (!captchaService.VerifyCaptcha(request.captcha)) {
            throw new Http_400_BadRequestException("验证码错误");
        }
        log.info("验证码校验通过:" + request.captcha);

        log.info("用户请求登录：" + request.username);

        Token token = new Token();

        token.token_type = "Bearer";

        TokenData tokenData;
        token.access_token = userService.login(request.username, request.password);
        WebUtils.respWrite(resp, token);
    }

    static class LoginRequest {
        @Regex("password")
        public String grant_type;
        @Regex("^[a-zA-Z0-9_-]{3,16}$")
        public String username;
        @Regex("^[a-zA-Z0-9_-]{6,18}$")
        public String password;
        public String captcha;
    }
}

