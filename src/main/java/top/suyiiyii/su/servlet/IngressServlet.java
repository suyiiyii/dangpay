package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.IOC.IOCmanager;
import top.suyiiyii.su.exception.Http_404_NotFoundException;

import java.io.IOException;
@Slf4j
@WebServlet("/")
public class IngressServlet extends HttpServlet {

    private static final String SERVLET_PACKAGENAME = "top.suyiiyii.servlet";

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
        // 创建IOC管理器
        IOCmanager IOCmanager = new IOCmanager();
        // 创建对象，通过依赖注入管理器获取对应的servlet
        // 获取调用的路径
        String path = req.getRequestURI();
        // 获取类名
        String className = path.replace("/", ".");
        // 获取类的全限定名，把路径的最后一部分作为类名，首字母大写
        // 例如：/user -> top.suyiiyii.servlet.User
        //TODO：路径最后一段可以是数字
        String fullClassName = SERVLET_PACKAGENAME + "." + className.substring(className.lastIndexOf(".") + 1).toUpperCase().charAt(0) + className.substring(className.lastIndexOf(".") + 2);
        log.info("请求路径：" + path + "，类名：" + fullClassName);
        // 通过反射创建对象
        BaseHttpServlet servlet;
        try {
            servlet = IOCmanager.createObj(fullClassName);
        } catch (ClassNotFoundException e) {
            throw new Http_404_NotFoundException("404 Not Found");
        }
        // 执行方法，调用servlet的service方法
        servlet.service(req, resp);
        // 销毁对象，递归调用字段的destroy方法
        IOCmanager.destroyObj(servlet);
    }
}
