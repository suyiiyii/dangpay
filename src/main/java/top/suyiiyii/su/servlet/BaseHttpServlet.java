package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.orm.core.Session;

import java.io.IOException;

/**
 * BaseHttpServlet
 * 用于servlet的基类，提供了数据库会话和配置管理器，实现简单的依赖注入
 *
 * @author suyiiyii
 */

@Slf4j
public class BaseHttpServlet extends HttpServlet {
    protected Session db;
    protected ConfigManger configManger;
    protected int uid = -1;
    protected String role = "guest";

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
        if ("GET".equals(req.getMethod())) {
            doGet(req, resp);
        } else if ("POST".equals(req.getMethod())) {
            doPost(req, resp);
        } else if ("PUT".equals(req.getMethod())) {
            doPut(req, resp);
        } else if ("DELETE".equals(req.getMethod())) {
            doDelete(req, resp);
        } else if ("PATCH".equals(req.getMethod())) {
            doPatch(req, resp);
        } else {
            resp.setStatus(405);
            resp.getWriter().write("Method Not Allowed");
        }
    }


    private void inject(HttpServletRequest req) {
//        ServletConfig config = getServletConfig();
//        this.db = ((ModelManger) config.getServletContext().getAttribute("ModelManger")).getSession();
//        this.configManger = (ConfigManger) config.getServletContext().getAttribute("ConfigManger");
//        TokenData tokenData = (TokenData) req.getAttribute("tokenData");
//        if (tokenData != null) {
//            this.uid = tokenData.uid;
//            this.role = tokenData.role;
//            if (this.role.equals("admin")) {
//                this.uid = -1;
//            }
//        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }
}

