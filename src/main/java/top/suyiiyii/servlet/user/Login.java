package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.Token;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.service.UserService;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.servlet.BaseHttpServlet;

import java.io.IOException;

@Slf4j
public class Login extends BaseHttpServlet {
    UserService userService;

    public Login(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        LoginRequest request = new LoginRequest();

        request.grant_type = req.getParameter("grant_type");
        request.username = req.getParameter("username");
        request.password = req.getParameter("password");

        log.info("用户请求登录：" + request.username);

        Token token = new Token();

        token.token_type = "Bearer";

        TokenData tokenData;
        token.access_token = userService.login(request.username, request.password);
        WebUtils.respWrite(resp, token);
    }
}

class LoginRequest {
    public String grant_type;
    public String username;
    public String password;
}

