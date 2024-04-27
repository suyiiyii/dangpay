package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.service.CaptchaService;
import top.suyiiyii.service.MailService;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.validator.Regex;

public class RequireCode {
    private final MailService mailService;
    private final CaptchaService captchaService;

    public RequireCode(MailService mailService,
                       CaptchaService captchaService) {
        this.mailService = mailService;
        this.captchaService = captchaService;
    }

    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        RequireCodeReqeust request = WebUtils.readRequestBody2Obj(req, RequireCodeReqeust.class);
        captchaService.verifyCaptcha(request.captcha);
        mailService.sendVerifyMail(request.email);
        return true;
    }


    @Data
    public static class RequireCodeReqeust {
        @Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        public String email;
        public String captcha;
    }
}
