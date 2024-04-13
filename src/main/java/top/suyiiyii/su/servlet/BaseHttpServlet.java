package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.orm.core.ModelManger;
import top.suyiiyii.su.orm.core.Session;

import java.io.IOException;

/**
 * BaseHttpServlet
 * 用于servlet的基类，提供了数据库会话和配置管理器，实现简单的依赖注入
 *
 * @author suyiiyii
 */
public class BaseHttpServlet extends HttpServlet {
    protected Session db;
    protected ConfigManger configManger;
    protected int uid = -1;
    protected String role = "guest";
    protected int statusCode = 0;
    protected Log logger;

    /**
     * 依赖注入
     * 注意： ServletConfig是全局上下文，用于保存全局信息；而HttpServletRequest是请求上下文，用于保存请求信息
     * init方法是在servlet初始化时调用的，service方法是在每次请求时调用的
     *
     * @param req  the {@link HttpServletRequest} object that contains the request the client made of the servlet
     * @param resp the {@link HttpServletResponse} object that contains the response the servlet returns to the client
     * @throws ServletException if an exception occurs that interferes with the servlet's normal operation
     * @throws IOException      if an input or output exception occurs
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        inject(req);
        try {
            if ("PATCH".equals(req.getMethod())) {
                doPatch(req, resp);
            } else {
                super.service(req, resp);
            }
        } finally {
            db.close();
            req.setAttribute("statusCode", statusCode);
        }
    }

    /**
     * 依赖注入
     * 功能同上
     * 为了解决依赖注入的顺序问题，提供回调函数接口，在上层依赖注入后再注入下层依赖
     * //PROBLEM 这里遇到了依赖注入的顺序问题，如果在service方法中注入，会导致在doPatch方法中无法使用db，所以提供了回调函数接口，有更好的解决方法吗？
     *
     * @param req      the {@link HttpServletRequest} object that contains the request the client made of the servlet
     * @param resp     the {@link HttpServletResponse} object that contains the response the servlet returns to the client
     * @param callable the {@link Runnable} object that contains the code to be executed
     * @throws ServletException if an exception occurs that interferes with the servlet's normal operation
     * @throws IOException      if an input or output exception occurs
     */
    protected void service(HttpServletRequest req, HttpServletResponse resp, Runnable callable) throws IOException, ServletException {

        inject(req);
        try {
            callable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            if ("PATCH".equals(req.getMethod())) {
                doPatch(req, resp);
            } else {
                super.service(req, resp);
            }
        } finally {
            db.close();
            req.setAttribute("statusCode", statusCode);
        }
    }

    private void inject(HttpServletRequest req) {
        logger = LogFactory.getLog(this.getClass());
        ServletConfig config = getServletConfig();
        this.db = ((ModelManger) config.getServletContext().getAttribute("ModelManger")).getSession();
        this.configManger = (ConfigManger) config.getServletContext().getAttribute("ConfigManger");
        TokenData tokenData = (TokenData) req.getAttribute("tokenData");
        if (tokenData != null) {
            this.uid = tokenData.uid;
            this.role = tokenData.role;
            if (this.role.equals("admin")) {
                this.uid = -1;
            }
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }
}

