package top.suyiiyii.su;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import top.suyiiyii.su.exception.Http_401_UnauthorizedException;

import java.io.IOException;
import java.util.Date;

/**
 * jwt工具类
 * 用于生成token和验证token
 * 依赖auth0的jwt库
 *
 * @author suyiiyii
 */
public class JwtUtils {
    /**
     * 创建token
     *
     * @param data      数据
     * @param secret    密钥
     * @param expSecond 过期时间
     * @return token
     * @throws IOException 异常
     */
    public static String createToken(Object data, String secret, int expSecond) {
        try {
            String sub = UniversalUtils.obj2Json(data);
            long exp = System.currentTimeMillis() + expSecond * 1000L;
            Date expDate = new Date(exp);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create().withSubject(sub).withExpiresAt(expDate).sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("create token failed");
        }
    }

    /**
     * 验证token
     *
     * @param token  token
     * @param secret 密钥
     * @return token的sub
     * @throws IOException 异常
     */

    public static String verifyToken(String token, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWT.require(algorithm).build().verify(token);
            return JWT.decode(token).getClaim("sub").asString();
        } catch (Exception e) {
            throw new Http_401_UnauthorizedException("verify token failed");
        }
    }
}
