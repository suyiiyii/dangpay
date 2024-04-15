package top.suyiiyii.servlet.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.su.DI.DImanager;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.orm.core.Session;

import java.io.IOException;

@WebServlet("/user")
public class User extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Session db = DImanager.getObj(Session.class);
        WebUtils.respWrite(resp, "使用依赖注入获取数据库连接成功，" + db.toString());
    }
}
