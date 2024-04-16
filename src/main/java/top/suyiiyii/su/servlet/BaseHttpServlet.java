package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_404_NotFoundException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * BaseHttpServlet
 * 用于servlet的基类，添加了Patch方法
 *
 * @author suyiiyii
 */

@Slf4j
public class BaseHttpServlet extends HttpServlet {

    private IngressServlet.SubMethod subMethod;

    public BaseHttpServlet(IngressServlet.SubMethod subMethod) {
        this.subMethod = subMethod;
    }

    public BaseHttpServlet() {
    }


    /**
     * 收到一个http请求，自动路由到本实例的对应的操作方法
     *
     * @param req  the {@link HttpServletRequest} object that contains the request the client made of the servlet
     * @param resp the {@link HttpServletResponse} object that contains the response the servlet returns to the client
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }


    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }
}

