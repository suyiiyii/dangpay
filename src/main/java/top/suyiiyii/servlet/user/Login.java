package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.Token;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.validator.Regex;
import top.suyiiyii.su.validator.Validator;

import java.io.IOException;

@Slf4j
public class Login {
    UserService userService;

    public Login(UserService userService) {
        super();
        this.userService = userService;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        LoginRequest request = new LoginRequest();

        request.grant_type = req.getParameter("grant_type");
        request.username = req.getParameter("username");
        request.password = req.getParameter("password");
        Validator.check(request);

        log.info("用户请求登录：" + request.username);

        Token token = new Token();

        token.token_type = "Bearer";

        TokenData tokenData;
        token.access_token = userService.login(request.username, request.password);
        WebUtils.respWrite(resp, token);
    }
}

class LoginRequest {
    @Regex("password")
    public String grant_type;
    @Regex("^[a-zA-Z0-9_-]{3,16}$")
    public String username;
    @Regex("^[a-zA-Z0-9_-]{6,18}$")
    public String password;
}

