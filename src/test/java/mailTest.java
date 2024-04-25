import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.MailSender;

public class mailTest {

    public static void main(String[] args) {
        ConfigManger configManger = new ConfigManger("application.yaml");

        // 邮件服务
        MailSender mailSender = new MailSender(
                configManger.get("MAIL_HOST"),
                configManger.get("MAIL_PORT"),
                configManger.get("MAIL_USERNAME"),
                configManger.get("MAIL_PASSWORD")
        );

        mailSender.sendMail("dujiakai@foxmail.com", "测试邮件", "这是一封测试邮件");

    }
}
