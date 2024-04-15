package top.suyiiyii.su.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.WebUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * 异常处理过滤器
 * 捕获所有异常并返回异常信息
 *
 * @author suyiiyii
 */
@Slf4j
public class ExceptionHandlerFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        log.info("开始处理请求： " + req.getRequestURI());
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            // 将异常堆栈跟踪信息转换为换行符分隔的字符串
            String stackTrace = Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
            log.error("请求处理失败： " + e.getMessage() + "\n" + stackTrace);
            HttpServletResponse resp = (HttpServletResponse) servletResponse;
            // 暂时不考虑使用不同的状态码，待后续使用Spring等框架再进行优化
            resp.setStatus(500);
            try {
                if ((int) req.getAttribute("statusCode") != 0) {
                    resp.setStatus((int) req.getAttribute("statusCode"));
                }
            } catch (Exception ignored) {
            }
            WebUtils.respWrite(resp, e.getMessage());
        }
        log.info("请求处理完成： " + req.getRequestURI());
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
