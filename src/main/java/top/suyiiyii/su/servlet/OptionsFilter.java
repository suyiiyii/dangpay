package top.suyiiyii.su.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.su.WebUtils;

import java.io.IOException;

/**
 * 允许所有OPTIONS请求通过
 * 解决跨域问题
 *
 * @author suyiiyii
 */
@WebFilter("/*")
public class OptionsFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        if ("OPTIONS".equals(req.getMethod())) {
            WebUtils.respWrite((HttpServletResponse) servletResponse, "ok from optionsFilter");
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
