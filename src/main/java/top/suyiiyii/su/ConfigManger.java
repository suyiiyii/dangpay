package top.suyiiyii.su;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import top.suyiiyii.su.exception.ConfigNotFoundException;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置文件管理器
 * 读取配置文件，如果没有找到则从环境变量中读取
 * 如果环境变量中也没有找到则抛出异常
 *
 * @author suyiiyii
 */
public class ConfigManger {
    /**
     * 线程安全：使用ConcurrentHashMap，同时使用computeIfAbsent方法，保证线程安全
     */
    private static final Log logger = LogFactory.getLog(ConfigManger.class);
    Map<String, String> config = new ConcurrentHashMap<>();

    public ConfigManger(String propertiesPath) {
        // 读取配置文件
        try {
            logger.info("尝试读取配置文件 " + propertiesPath);
            InputStream in = ConfigManger.class.getClassLoader().getResourceAsStream(propertiesPath);
            java.util.Properties properties = new java.util.Properties();
            properties.load(in);
            for (String key : properties.stringPropertyNames()) {
                config.put(key, properties.getProperty(key));
            }
            logger.info("读取配置文件成功，一共 " + config.size() + " 条配置");
        } catch (Exception e) {
            logger.info("读取配置文件失败，将会从环境变量读取配置 " + e);
        }
    }

    public String get(String key) {
        return config.computeIfAbsent(key, k -> {
            logger.error("配置文件中没有找到 " + key + " ，将会从环境变量读取");
            String value = System.getenv(key);
            if (value == null) {
                logger.error("环境变量中没有找到 " + key);
                throw new ConfigNotFoundException("配置文件和环境变量中都没有找到 " + key);
            }
            return value;
        });
    }
}

