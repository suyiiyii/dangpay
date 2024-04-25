package top.suyiiyii.service;

import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.MailSender;

public class MailService {
    private final MailSender mailSender;
    private final ConfigManger configManger;

    public MailService(MailSender mailSender, ConfigManger configManger) {
        this.mailSender = mailSender;
        this.configManger = configManger;
    }

    public void sendMail(String to, String subject, String content) {
        mailSender.sendMail(to, subject, content);
    }

    public void sendVerifyMail(String to, String code) {
        String subject = configManger.get("PLATFORM_NAME") + "邮箱验证";
        String content = "您的验证码是：" + code;
        sendMail(to, subject, content);
    }
}
