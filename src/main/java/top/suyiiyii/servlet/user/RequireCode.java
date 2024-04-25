package top.suyiiyii.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import top.suyiiyii.service.MailService;
import top.suyiiyii.su.WebUtils;
import top.suyiiyii.su.validator.Regex;

public class RequireCode {
    private final MailService mailService;

    public RequireCode(MailService mailService) {
        this.mailService = mailService;
    }

    public boolean doPost(HttpServletRequest req, HttpServletResponse resp) {
        RequireCodeReqeust request = WebUtils.readRequestBody2Obj(req, RequireCodeReqeust.class);
        mailService.sendVerifyMail(request.email);
        return true;
    }


    @Data
    public static class RequireCodeReqeust {
        @Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        public String email;
    }
}
