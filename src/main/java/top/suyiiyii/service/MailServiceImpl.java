package top.suyiiyii.service;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.suyiiyii.dao.TransactionDao;
import top.suyiiyii.models.User;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.MailSender;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.time.Duration;
import java.util.NoSuchElementException;

@Repository
public class MailServiceImpl implements MailService {
    private final MailSender mailSender;
    private final ConfigManger configManger;
    private final RedissonClient redissonClient;
    private final TransactionDao transactionDao;
    private final Session db;

    public MailServiceImpl(MailSender mailSender,
                           ConfigManger configManger,
                           RedissonClient redissonClient,
                           TransactionDao transactionDao,
                           Session db) {
        this.mailSender = mailSender;
        this.configManger = configManger;
        this.redissonClient = redissonClient;
        this.transactionDao = transactionDao;
        this.db = db;
    }

    @Override
    public void sendMail(String to, String subject, String content) {
        String keyPrefix = "mailCount_";
        long now = System.currentTimeMillis();
        String key = keyPrefix + to + now;
        // 检查过去24小时发送的邮件数量
        int count = 0;
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(keyPrefix + "*");
        for (String k : keys) {
            count++;
        }
        if (count >= 100) {
            throw new Http_400_BadRequestException("邮件发送数量超过24小时内的100封限制");
        }
        // 发送邮件
        mailSender.sendMail(to, subject, content);
        // 创建一个新的key，并在24小时后过期
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set("sent", Duration.ofDays(1));
    }

    @Override
    public void sendVerifyMail(String email) {
        // 检查是邮箱是否已经注册
        if (db.query(User.class).eq("email", email).exists()) {
            throw new Http_400_BadRequestException("邮箱已被注册，请更换邮箱");
        }
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
