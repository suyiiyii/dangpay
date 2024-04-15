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
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        log.info("开始处理请求： " + req.getRequestURI());
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            // 将异常堆栈跟踪信息转换为换行符分隔的字符串
            String stackTrace = Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
            log.error("请求处理失败： " + e.getMessage() + "\n" + e.getClass().getName() + "\n" + stackTrace);

            int flag = -1;
            switch (e.getClass().getSimpleName()) {
                case "Http_400_BadRequestException":
                    resp.setStatus(400);
                    break;
                case "Http_401_UnauthorizedException":
                    resp.setStatus(401);
                    break;
                case "Http_403_ForbiddenException":
                    resp.setStatus(403);
                    break;
                case "Http_404_NotFoundException":
                    resp.setStatus(404);
                    break;
                case "Http_405_MethodNotAllowedException":
                    resp.setStatus(405);
                    break;
                default:
                    resp.setStatus(500);
                    flag = 1;
                    break;
            }
            if (flag == 1) {
                WebUtils.respWrite(resp, "服务器内部错误");
            } else {
                WebUtils.respWrite(resp, e.getMessage());
            }
        }
        log.info("请求处理完成： " + req.getRequestURI());
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
