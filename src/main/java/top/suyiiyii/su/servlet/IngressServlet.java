package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.su.IOC.IOManager;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.exception.Http_404_NotFoundException;
import top.suyiiyii.su.orm.core.ModelManger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

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
        IOManager ioManager = new IOManager();
        // 创建对象，通过依赖注入管理器获取对应的servlet
        // 获取调用的路径
        String path = req.getRequestURI();
        // 获取类名
        String[] paths = (path.endsWith("/") ? path.substring(0, path.length() - 1) : path).split("/");
        // 判断路径是否以数字结尾
        // 路径支持嵌套
        String fullClassName;
        int id = -1;
        SubMethod subMethod = new SubMethod();
        if (paths[paths.length - 1].matches(".*\\d.*")) {
            fullClassName = SERVLET_PACKAGENAME + String.join(".", Arrays.copyOfRange(paths, 0, paths.length - 1)) + "ID";
            subMethod.id = Integer.parseInt(paths[paths.length - 1]);
        } else if (paths.length >= 3 && paths[paths.length - 2].matches(".*\\d.*")) {
            fullClassName = SERVLET_PACKAGENAME + String.join(".", Arrays.copyOfRange(paths, 0, paths.length - 2)) + "ID";
            subMethod.id = Integer.parseInt(paths[paths.length - 2]);
            subMethod.name = paths[paths.length - 1];
        } else {
            fullClassName = SERVLET_PACKAGENAME + String.join(".", Arrays.copyOfRange(paths, 0, paths.length));
        }
        // 全限定名最后一个单词首字母大写
        String[] fullClassNames = fullClassName.split("\\.");
        fullClassNames[fullClassNames.length - 1] = fullClassNames[fullClassNames.length - 1].substring(0, 1).toUpperCase() + fullClassNames[fullClassNames.length - 1].substring(1);
        fullClassName = String.join(".", fullClassNames);

        log.info("请求路径：" + path + "，类名：" + fullClassName + "，id：" + id + "，子方法：" + subMethod.getName());

        // 添加本地依赖，tokenData
        TokenData tokenData = (TokenData) req.getAttribute("tokenData");
        ioManager.registerLocalBean(tokenData);
        ioManager.registerLocalBean(IOManager.getGlobalBean(ModelManger.class).getSession());
        ioManager.registerLocalBean(subMethod);
        ioManager.registerLocalBean(ioManager.getObj(UserRoles.class));
        ioManager.registerLocalBean(ioManager);

        log.info("tokenData：" + tokenData + "，userRoles：" + ioManager.getObj(UserRoles.class));

        // 通过反射创建对象
        Object servlet;
        try {
            servlet = ioManager.createObj(fullClassName);
        } catch (ClassNotFoundException e) {
            throw new Http_404_NotFoundException("404 Not Found");
        }
        try {
            // 执行方法，调用servlet的service方法
            methodGateway(req, resp, subMethod, servlet);
        } finally {
            // 销毁对象，递归调用字段的destroy方法
            ioManager.destroyObj(servlet);
            ioManager.destroy();
        }
    }

    @SneakyThrows
    private void methodGateway(HttpServletRequest req, HttpServletResponse resp, SubMethod subMethod, Object obj) {
        String methodName = "do" + UniversalUtils.capitalizeFirstLetter(req.getMethod().toLowerCase());
        if (subMethod != null && subMethod.getName() != null) {
            methodName += UniversalUtils.capitalizeFirstLetter(subMethod.getName());
        }
        try {
            // 通过反射调用对应的方法
            Method method = null;
            Object ret = null;
            try {
                method = obj.getClass().getDeclaredMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
                method.setAccessible(true);
                ret = method.invoke(obj, req, resp);
            } catch (NoSuchMethodException e) {
                method = obj.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                ret = method.invoke(obj);
            }
            if (ret != null) {
                WebUtils.respWrite(resp, ret);
            }
        } catch (InvocationTargetException e) {
            // invoke方法抛出的是一个包装过的异常，需要通过getTargetException获取原始异常
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new Http_404_NotFoundException();
        }
    }

    @Data
    public static class SubMethod {
        int id;
        String name;
    }
}
