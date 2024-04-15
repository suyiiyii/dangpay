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
        log.info("JwtFilter: " + req.getRequestURI());
        // 白名单机制，跳过登录注册接口以及健康检查接口
        if (req.getRequestURI().equals("/user/login") ||
                req.getRequestURI().equals("/user/register") ||
                req.getRequestURI().equals("/health_check")) {
            log.info("跳过登录注册接口");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        String authHeader = req.getHeader("Authorization");
        if ("GET".equals(req.getMethod())) {
            log.info("跳过纯获取信息接口");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            servletRequest.setAttribute("statusCode", 401);
            throw new ServletException("缺少Authorization头或者Authorization头不以Bearer开头");
        }
        String token = authHeader.substring(7);

        // 验证token
        String tokenStr = verifyToken(token, configManger.get("secret"));

        // 注入tokenData
        TokenData tokenData = UniversalUtils.json2Obj(tokenStr, TokenData.class);
        req.setAttribute("tokenData", tokenData);
        log.info("token verify success");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}

