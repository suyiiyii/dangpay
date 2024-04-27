package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.exception.Http_400_BadRequestException;

@Slf4j
@Repository
public class CaptchaServiceImpl implements CaptchaService {
    ConfigManger configManger;

    public CaptchaServiceImpl(ConfigManger configManger) {
        this.configManger = configManger;
    }

    @Override
    public boolean verifyCaptcha(String captcha) {
        // 跳过非dangpay平台的验证
//        if (!configManger.get("PLATFORM_NAME").equals("dangpay")) {
//            log.info("当前平台是{}，跳过验证码验证", configManger.get("PLATFORM_NAME"));
//            return true;
//        }
        log.info("开始验证人机验证码" + captcha);
        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        String secret = configManger.get("CF_SECRET_KEY");
        try {
            FormBody formBody = new FormBody.Builder()
                    .add("secret", secret)
                    .add("response", captcha)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            OkHttpClient client = new OkHttpClient();

            okhttp3.Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                log.info("人机验证码验证结果" + body);
                if (body.contains("\"success\":true")) {
                    return true;
                }
            }
            log.info("人机验证码验证失败" + captcha);
            throw new Http_400_BadRequestException("人机验证码验证失败，请刷新页面后重试");
        } catch (Exception ignored) {
        }
        log.info("人机验证码验证失败" + captcha);
        throw new Http_400_BadRequestException("人机验证码验证失败，请刷新页面后重试");
    }
}
