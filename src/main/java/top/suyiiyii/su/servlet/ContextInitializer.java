package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.IOCmanager;
import top.suyiiyii.su.orm.core.ModelManger;
import top.suyiiyii.su.orm.utils.ConnectionBuilder;

/**
 * 依赖注入
 * 注入部分依赖到 ServletContext
 *
 * @author suyiiyii
 */
@Slf4j
@WebListener
public class ContextInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("初始化依赖注入");

        ConfigManger configManger = new ConfigManger("application.properties");
        String url = configManger.get("JDBC_URL");
        String user = configManger.get("JDBC_USER");
        String password = configManger.get("JDBC_PASSWORD");
        log.info("JDBC_URL: {}", url);
        log.info("JDBC_USER: {}", user);

        ConnectionBuilder builder = new ConnectionBuilder(url, user, password);

        ModelManger modelManger = new ModelManger("top.suyiiyii.models", builder::getConnection);

        ServletContext servletContext = sce.getServletContext();
        servletContext.setAttribute("ModelManger", modelManger);
        servletContext.setAttribute("ConfigManger", configManger);
        IOCmanager.registerGlobalBean(modelManger);
        IOCmanager.registerGlobalBean(configManger);
        log.info("依赖注入完成");
    }
}
