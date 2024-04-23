package top.suyiiyii.su.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.IOCManager;
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

        ConfigManger configManger = new ConfigManger("application.yaml");
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
        IOCManager.registerGlobalBean(modelManger);
        IOCManager.registerGlobalBean(configManger);
        IOCManager.implScan("top.suyiiyii.service");
        // redis
        Config config = new Config();
        if (configManger.get("REDIS_PASSWORD").isEmpty()) {
            config.useSingleServer()
                    .setAddress(configManger.get("REDIS_URL"));
        } else {
            config.useSingleServer()
                    .setAddress(configManger.get("REDIS_URL"))
                    .setPassword(configManger.get("REDIS_PASSWORD"));
        }
        log.error("REDIS_URL: {}", configManger.get("REDIS_URL"));
        log.error("REDIS_PASSWORD: {}", configManger.get("REDIS_PASSWORD"));
        RedissonClient redisson = Redisson.create(config);
        IOCManager.registerGlobalBean(redisson);
        // 由于RedissonClient是接口，所以需要注册实现类
        IOCManager.registerInterface2Impl(RedissonClient.class, Redisson.class);

        log.info("依赖注入完成");
    }
}
