package top.suyiiyii.service;

import org.redisson.api.RedissonClient;
import top.suyiiyii.dao.TransactionDao;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.MailSender;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;

import java.time.Duration;
import java.util.NoSuchElementException;

public class MailServiceImpl implements MailService {
    private final MailSender mailSender;
    private final ConfigManger configManger;
    private final RedissonClient redissonClient;
    private final TransactionDao transactionDao;

    public MailServiceImpl(MailSender mailSender,
                           ConfigManger configManger,
                           RedissonClient redissonClient,
                           TransactionDao transactionDao) {
        this.mailSender = mailSender;
        this.configManger = configManger;
        this.redissonClient = redissonClient;
        this.transactionDao = transactionDao;
    }

    @Override
    public void sendMail(String to, String subject, String content) {
        mailSender.sendMail(to, subject, content);
    }

    @Override
    public void sendVerifyMail(String email) {
        String key = "verifyCode_" + email;
        String keyStamp = "verifyCode_stamp" + email;
        // 60s内只能发送一次
        try {
            String stamp = transactionDao.get(keyStamp);
            if (UniversalUtils.getNow() - Long.parseLong(stamp) < 60) {
                throw new Http_400_BadRequestException("请勿频繁发送验证码，60s后再试");
            }
        } catch (NoSuchElementException e) {
        }
        // 生成验证码
        String code = generateCode();
        // 缓存验证码
        transactionDao.insert(key, code, Duration.ofMinutes(5));
        transactionDao.insert(keyStamp, UniversalUtils.getNow(), Duration.ofMinutes(5));
        // 发送邮件
        String subject = configManger.get("PLATFORM_NAME") + "邮箱验证";
        String content = "您的验证码是：" + code;
        sendMail(email, subject, content);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String key = "verifyCode_" + email;
        String cacheCode;
        if (transactionDao.getKeyExpiry(key) < 0) {
            throw new Http_400_BadRequestException("验证码不存在或已过期，请重新发送");
        }
        cacheCode = transactionDao.get(key);
        return code.equals(cacheCode);
    }


    /**
     * 生成验证码
     * 4位数字
     *
     * @return 验证码
     */
    @Override
    public String generateCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 1000));
    }
}
