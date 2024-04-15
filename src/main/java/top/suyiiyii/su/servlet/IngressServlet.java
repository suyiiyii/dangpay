package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.servlet.User;
import top.suyiiyii.su.DI.DImanager;

import java.io.IOException;

@WebServlet("/")
public class IngressServlet extends HttpServlet {

    /**
     * 接受请求并且路由到对应的servlet
     * 实现依赖注入和简单的ioc容器生命周期管理
     *
     * @param req  the {@link HttpServletRequest} object that contains the request the client made of the servlet
     * @param resp the {@link HttpServletResponse} object that contains the response the servlet returns to the client
     * @throws ServletException if an exception occurs that interferes with the servlet's normal operation
     * @throws IOException      if an input or output error is detected when the servlet handles the request
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 创建对象，通过依赖注入管理器获取对应的servlet
        BaseHttpServlet servlet = DImanager.getObj(User.class);
        // 执行方法，调用servlet的service方法
        servlet.service(req, resp);
        // 销毁对象，递归调用字段的destroy方法
        DImanager.destroyObj(servlet);
    }
}
