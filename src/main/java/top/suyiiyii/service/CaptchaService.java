package top.suyiiyii.service;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.exception.Http_400_BadRequestException;

public class CaptchaService {
    ConfigManger configManger;

    public CaptchaService(ConfigManger configManger) {
        this.configManger = configManger;
    }

    public boolean VerifyCaptcha(String captcha) {
        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        String secret = configManger.get("CF_SECRET_KEY");
        FormBody formBody = new FormBody.Builder()
                .add("secret", secret)
                .add("response", captcha)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        OkHttpClient client = new OkHttpClient();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                return body.contains("\"success\":true");
            }
        } catch (Exception ignored) {
        }
        throw new Http_400_BadRequestException("验证码验证失败");

    }

}
