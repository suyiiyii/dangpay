package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.models.User;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.JwtUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.NoSuchElementException;

@Slf4j
public class UserService {
    Session db;
    ConfigManger configManger;

    public UserService(Session db,
                       ConfigManger configManger) {
        this.db = db;
        this.configManger = configManger;
    }

    public static String getHashed(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            log.error("md5算法不可用");
            throw new RuntimeException("md5算法不可用");
        }
        md.update(password.getBytes());
        md.update("suyiiyii".getBytes());
        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static boolean checkPassword(String password, User user) {
        return user.getPassword().equals(getHashed(password));
    }

    /**
     * 登录，返回
     *
     * @param username
     * @param password
     * @return
     */

    public String login(String username, String password) {
        User user = null;
        try {
            user = db.query(User.class).eq("username", username).first();
        } catch (NoSuchElementException | SQLException e) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        if (!checkPassword(password, user)) {
            throw new Http_400_BadRequestException("密码错误");
        }
        TokenData tokenData = new TokenData();
        tokenData.uid = user.getId();
        String token = JwtUtils.createToken(tokenData, configManger.get("secret"), 60 * 60 * 24 * 7);
        log.info("签发token：" + token);
        return token;
    }

    public User getUser(int uid) {
        try {
            return db.query(User.class).eq("id", uid).first();
        } catch (SQLException e) {
            throw new Http_400_BadRequestException("用户不存在");
        }
    }

    public User register(String username, String password, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(getHashed(password));
        user.setPhone(phone);
        user.setIconUrl("");
        user.setStatus("normal");
        boolean isExist = true;
        try {
            db.query(User.class).eq("username", username).first();
        } catch (NoSuchElementException | SQLException e) {
            isExist = false;
        }
        if (isExist) {
            throw new Http_400_BadRequestException("用户名已存在");
        }
        try {
            db.insert(user);
        } catch (SQLException e) {
            throw new Http_400_BadRequestException("注册失败");
        }
        return user;
    }
}

