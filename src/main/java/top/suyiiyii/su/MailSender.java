package top.suyiiyii.su;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailSender {


    private final String MAIL_HOST;
    private final String MAIL_PORT;
    private final String MAIL_USERNAME;
    private final String MAIL_PASSWORD;

    public MailSender(String MAIL_HOST, String MAIL_PORT, String MAIL_USERNAME, String MAIL_PASSWORD) {
        this.MAIL_HOST = MAIL_HOST;
        this.MAIL_PORT = MAIL_PORT;
        this.MAIL_USERNAME = MAIL_USERNAME;
        this.MAIL_PASSWORD = MAIL_PASSWORD;
    }

    public void sendMail(String to, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.host", MAIL_HOST);
        props.put("mail.smtp.port", MAIL_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", MAIL_HOST);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.required", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MAIL_USERNAME, MAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(MAIL_USERNAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
