package top.suyiiyii.su.exception;

/**
 * 找不到配置文件异常
 *
 * @author suyiiyii
 */
public class ConfigNotFoundException extends RuntimeException {
    public ConfigNotFoundException(String message) {
        super(message);
    }
}
