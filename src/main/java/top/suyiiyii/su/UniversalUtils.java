package top.suyiiyii.su;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 通用工具类
 *
 * @author suyiiyii
 */
@Slf4j
public class UniversalUtils {
    /**
     * 把对象转换成json字符串
     *
     * @param object Object
     * @return String
     * @throws IOException IOException
     */
    public static String obj2Json(Object object) {
        try {
            return WebUtils.MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 把json字符串转换成对象
     *
     * @param json      String
     * @param valueType Class<T>
     * @param <T>       T
     * @return T
     * @throws IOException IOException
     */

    public static <T> T json2Obj(String json, Class<T> valueType) {
        try {
            return WebUtils.MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下划线转小驼峰
     *
     * @param downLineStr String
     * @return String
     */
    public static String downToCaml(String downLineStr) {
        StringBuilder builder = new StringBuilder();
        boolean isAfterDownLine = false;
        for (int i = 0; i < downLineStr.length(); i++) {
            char c = downLineStr.charAt(i);
            if (c == '_') {
                isAfterDownLine = true;
                continue;
            }
            if (isAfterDownLine) {
                c = Character.toUpperCase(c);
                isAfterDownLine = false;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    /**
     * 小驼峰转下划线
     *
     * @param camlStr String
     * @return String
     */
    public static String camlToDown(String camlStr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < camlStr.length(); i++) {
            char c = camlStr.charAt(i);
            if (Character.isUpperCase(c)) {
                builder.append("_");
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 浅拷贝一个对象
     *
     * @param obj T
     * @param <T> T
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T obj) {
        try {
            T cloneObj = (T) obj.getClass().newInstance();
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                field.set(cloneObj, value);
            }
            return cloneObj;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断两个对象是否相等
     * 通过反射获取所有字段，逐一比较
     * 仅支持基本数据类型和String
     *
     * @param x Object
     * @param y Object
     * @return boolean
     */
    public static boolean equal(Object x, Object y) {
        if (x == y) {
            return true;
        }
        if (x.getClass() != y.getClass()) {
            return false;
        }
        try {
            Field[] fields = x.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object obj1 = field.get(x);
                Object obj2 = field.get(y);
                if (obj1 == null || obj2 == null) {
                    return false;
                }
                if (!obj1.equals(obj2)) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 使用反射，用新对象的非空属性更新旧对象的对应属性
     * 新旧类型可以不一致
     * 如果新对象存在而旧对象不存在的属性，会被忽略
     */
    public static void updateObj(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) {
            throw new IllegalArgumentException("对象不能为空");
        }
        if (oldObj.getClass() == newObj.getClass()) {
            try {
                Field[] fields = oldObj.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(newObj);
                    //判断是不是null
                    if (value == null) {
                        continue;
                    }
                    //判断是不是0
                    if (field.getType().equals(int.class) && (int) value == 0) {
                        continue;
                    }
                    field.set(oldObj, value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                Field[] oldFields = oldObj.getClass().getDeclaredFields();
                Field[] newFields = newObj.getClass().getDeclaredFields();
                for (Field oldField : oldFields) {
                    oldField.setAccessible(true);
                    for (Field newField : newFields) {
                        newField.setAccessible(true);
                        if (oldField.getName().equals(newField.getName())) {
                            Object value = newField.get(newObj);
                            if (value == null) {
                                continue;
                            }
                            if (newField.getType().equals(int.class) && (int) value == 0) {
                                continue;
                            }
                            oldField.set(oldObj, value);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char firstChar = str.charAt(0);
        char capitalizedFirstChar = Character.toUpperCase(firstChar);
        return capitalizedFirstChar + str.substring(1);
    }

    /**
     * AES加密
     * 把字符串加密成base64编码的字符串
     */
    public static String encrypt(String content, String secretKey) {
        try {
            // 把密钥转换成16字节的字节数组
            byte[] key = new byte[16];
            byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(secretKeyBytes, 0, key, 0, Math.min(secretKeyBytes.length, 16));
            // 创建一个AESKeySpec对象
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            // 创建一个全0的初始化向量
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);

            // 创建一个Cipher对象并初始化它，设置为加密模式
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // 加密数据
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            // 返回Base64编码的加密数据
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    /**
     * AES解密
     * 把base64编码的字符串解密成原始字符串
     */
    public static String decrypt(String base64Content, String secretKey) {
        try {
            // 创建一个AESKeySpec对象
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            // 创建一个全0的初始化向量
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);

            // 创建一个Cipher对象并初始化它，设置为解密模式
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // 解密数据
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(base64Content));

            // 返回解密后的数据
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }

    /**
     * rsa 签名
     * 传入要签名的字符串和私钥（PEM格式）
     * 返回签名后的base64字符串
     */
    public static String rsaSign(String content, String opensshPrivateKey) {

        opensshPrivateKey = opensshPrivateKey
                .replace("-----BEGIN OPENSSH PRIVATE KEY-----", "")
                .replace("-----END OPENSSH PRIVATE KEY-----", "")
                .replace(" ", "")
                .replace("\n", "");

        // base64解码
        byte[] keyData = Base64.getDecoder().decode(opensshPrivateKey);

        // 读取openssh格式的私钥
        AsymmetricKeyParameter asymmetricKeyParameter = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(keyData);

        // 构造签名器
        RSAKeyParameters rsaKeyParam = (RSAKeyParameters) asymmetricKeyParameter;
        RSADigestSigner signer = new RSADigestSigner(new SHA512Digest());
        signer.init(true, rsaKeyParam);

        // 生成签名
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        signer.update(data, 0, data.length);
        byte[] signature = null;
        try {
            signature = signer.generateSignature();
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        log.info("签名长度: {}", signature.length);

        // 返回Base64编码的签名数据
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * rsa 签名校验
     * 传入原始字符串、签名后的base64字符串和公钥（ssh-rsa格式）
     */
    public static boolean rsaVerify(String content, String signed, String publicKey) {

        publicKey = publicKey.split(" ")[1];

        log.info("publicKey: {}", publicKey);
        // base64解码
        byte[] keyData = Base64.getDecoder().decode(publicKey);

        // 读取openssh格式的公钥
        AsymmetricKeyParameter asymmetricKeyParameter = OpenSSHPublicKeyUtil.parsePublicKey(keyData);

        // 构造签名器
        RSAKeyParameters rsaKeyParam = (RSAKeyParameters) asymmetricKeyParameter;
        RSADigestSigner signer = new RSADigestSigner(new SHA512Digest());
        signer.init(false, rsaKeyParam);

        // 生成签名
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        signer.update(data, 0, data.length);
        byte[] signature = Base64.getDecoder().decode(signed);
        log.info("签名长度: {}", signature.length);

        // 验证签名
        return signer.verifySignature(signature);

        /*
        daily(2024.4.21):
        花了4个多小时，前期到处找资料，发现好像没有什么库可以直接用，并且这方面的资料也少得可怜；ai一直叫我用pem的方式去解析，试了错了问ai要怎么改，还是叫我换一个库然后继续用解析pem的方式去解析；csdn上面垃圾一大堆。然后在stackoverflow上找，有个20年的说现在ssh会默认使用自己的格式，还没有java库能够支持，后面到了一个答案，说有库能搞，就去搜索答案相关的那个库。然后找那个要找的有关OPENSSH的库的类，结果搜的时候发现bouncycastle原来有一个OpenSSHPrivateKeyUtil，看了一源码，发现有几个magic，拿手上的openssh的公钥钥试base64解码试了一下，就出现了代码中的magic，所以就判断这个库是对的，然后就开始尝试去调用这几个方法，主要是具体细节看不懂，文档也看不懂，说要传key，但是不知道是传一个完整的key，还是解码之后的key，还是什么，idea也调试不了库里面的方法，所以调试了好久，最终是弄好了

        大概就是实现了一个用openssh格式的私钥签名，用ssh-rsa格式的公钥验签的方法，自己当时“觉得”因为ssh很常用，所以用ssh-kenygen生成key更方便一些，不过更多是不管怎么样是想搞出来吧，虽然花了很多时间，但是也学到了很多东西，也还行
        下次再也不用openssh的格式了，老老实实换成pem格式

         */
    }
}
