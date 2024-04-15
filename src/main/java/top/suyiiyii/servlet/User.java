package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.orm.core.Session;
import top.suyiiyii.su.servlet.BaseHttpServlet;

import java.io.IOException;

public class User extends BaseHttpServlet {
    private Session db;

    public User(Session db) {
        this.db = db;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebUtils.respWrite(resp, "使用依赖注入获取数据库连接成功，" + db.toString());
    }

}
