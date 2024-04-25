package top.suyiiyii.service;

public interface MailService {
    void sendMail(String to, String subject, String content);

    void sendVerifyMail(String email);

    boolean verifyCode(String email, String code);

    String generateCode();
}
