package top.suyiiyii.service;

import top.suyiiyii.su.IOC.Proxy;

@Proxy(isNeedAuthorization = true)
public interface MailService {
    void sendMail(String to, String subject, String content);

    void sendVerifyMail(String email);

    boolean verifyCode(String email, String code);

    String generateCode();
}
