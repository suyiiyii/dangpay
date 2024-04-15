package top.suyiiyii.su.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.UniversalUtils;

import java.io.IOException;

import static top.suyiiyii.su.JwtUtils.verifyToken;


/**
 * jwt 校验过滤器
 * 如果校验成功，注入tokenData到servletRequest上下文
 * 过滤器应该覆盖所有接口，但使用白名单跳过登录注册接口
 *
 * @author suyiiyii
 */
@Slf4j
public class JwtFilter implements Filter {
    ConfigManger configManger;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        configManger = (ConfigManger) filterConfig.getServletContext().getAttribute("ConfigManger");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 获取token
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        TokenData tokenData;
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 如果没有token，注入一个guest的tokenData
            tokenData = new TokenData();
            // 定义uid=-1为访客
            tokenData.uid = -1;
        } else {
            String token = authHeader.substring(7);
            // 验证token
            String tokenStr = verifyToken(token, configManger.get("SECRET"));
            // 注入tokenData
            tokenData = UniversalUtils.json2Obj(tokenStr, TokenData.class);
        }
        req.setAttribute("tokenData", tokenData);
        log.info("JwtFilter: 接收到用户请求，uid:" + tokenData.uid);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}

