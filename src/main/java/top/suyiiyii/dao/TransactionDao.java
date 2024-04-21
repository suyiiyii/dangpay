package top.suyiiyii.dao;


import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.suyiiyii.service.TransactionServiceImpl;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.UniversalUtils;

import java.time.Duration;
import java.util.NoSuchElementException;

/**
 * 平台向第三方平台发起交易请求，会返回一个code，下次发送请求时需要携带这个code
 * 同时code有过期时间，过期后无法使用
 * 而交易的过程是不连续的，所以无论是发起交易请求还是验证交易请求，都需要记录交易的基本信息，记录code
 * 因此提出TransactionDao，在Transaction创建之前记录交易的基本信息
 * <p>
 * 生成code需要缓存code和交易的基本信息的关系，以便后面验证code时使用
 * 收到code后，需要缓存code和收到的交易的基本信息的关系，发送交易请求时使用
 *
 * <p>
 * 具体实现：
 * 使用redis存储code，key为code，value为交易的基本信息（json编码）
 */
public class TransactionDao {
    private final ConfigManger configManger;
    private final RedissonClient redisson;

    public TransactionDao(ConfigManger configManger, RedissonClient redisson) {
        this.configManger = configManger;
        this.redisson = redisson;
    }

    public void insertSentCode(String code, TransactionServiceImpl.CodeInCache response) {
        String key = configManger.get("PLATFORM_NAME") + "_Sent_" + code;
        String value = UniversalUtils.obj2Json(response);
        Duration duration = Duration.ofMinutes(response.getExpiredAt() - UniversalUtils.getNow());
        insert(key, value, duration);
    }

    public TransactionServiceImpl.CodeInCache getSentCode(String code) {
        String key = configManger.get("PLATFORM_NAME") + "_Sent_" + code;
        return UniversalUtils.json2Obj(get(key), TransactionServiceImpl.CodeInCache.class);
    }

    public void deleteSentCode(String code) {
        String key = configManger.get("PLATFORM_NAME") + "_Sent_" + code;
        delete(key);
    }


    public void insertReceivedCode(String code, TransactionServiceImpl.RequestTransactionResponse response) {
        String key = configManger.get("PLATFORM_NAME") + "_Received_" + code;
        String value = UniversalUtils.obj2Json(response);
        Duration duration = Duration.ofMinutes(response.getExpiredAt() - UniversalUtils.getNow());
        insert(key, value, duration);
    }


    public TransactionServiceImpl.RequestTransactionResponse getReceivedCode(String code) {
        String key = configManger.get("PLATFORM_NAME") + "_Received_" + code;
        return UniversalUtils.json2Obj(get(key), TransactionServiceImpl.RequestTransactionResponse.class);
    }

    public void deleteReceivedCode(String code) {
        String key = configManger.get("PLATFORM_NAME") + "_Received_" + code;
        delete(key);
    }


    public void insert(String key, Object value, Duration duration) {
        RBucket<String> bucket = redisson.getBucket(key);
        bucket.set(UniversalUtils.obj2Json(value), duration);
    }

    public String get(String key) {
        RBucket<String> bucket = redisson.getBucket(key);
        String value = bucket.get();
        if (value == null) {
            throw new NoSuchElementException("key not found");
        }
        value = UniversalUtils.json2Obj(value, String.class);
        return value;
    }

    public void delete(String key) {
        RBucket<String> bucket = redisson.getBucket(key);
        bucket.delete();
    }

}
