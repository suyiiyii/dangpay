package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.TokenData;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.User;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.JwtUtils;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.exception.Http_403_ForbiddenException;
import top.suyiiyii.su.orm.core.Session;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;

@Repository
@Slf4j
public class UserServiceImpl implements UserService {
    Session db;
    ConfigManger configManger;
    RBACService rbacService;

    public UserServiceImpl(Session db, ConfigManger configManger, RBACService rbacService) {
        this.db = db;
        this.configManger = configManger;
        this.rbacService = rbacService;
    }

    static String getHashed(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
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

    static boolean checkPassword(String password, User user) {
        return user.getPassword().equals(getHashed(password));
    }

    /**
     * 登录，返回
     *
     * @param username
     * @param password
     * @return
     */

    @Override
    public String login(String username, String password) {
        User user = null;
        try {
            user = db.query(User.class).eq("username", username).first();
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        if (!checkPassword(password, user)) {
            throw new Http_400_BadRequestException("密码错误");
        }
        TokenData tokenData = new TokenData();
        tokenData.uid = user.getId();
        String token = JwtUtils.createToken(tokenData, configManger.get("SECRET"), 60 * 60 * 24 * 7);
        log.info("签发token：" + token);
        return token;
    }

    @Override
    public User getUser(int uid) {
        try {
            return db.query(User.class).eq("id", uid).first();
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("用户不存在");
        }
    }

    @Override
    public List<User> getUsers(UserRoles userRoles) {
//        rbacService.checkPermission(userRoles, "UserServiceGetUsers");
        return db.query(User.class).all();
    }

    @Override
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
        } catch (NoSuchElementException e) {
            isExist = false;
        }
        if (isExist) {
            throw new Http_400_BadRequestException("用户名已存在");
        }
        try {
            db.beginTransaction();
            db.insert(user);
            rbacService.addUserRole(user.getId(), "user");
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw new Http_400_BadRequestException("注册失败");
        }
        return user;
    }

    @Override
    public User banUser(int uid) {
        User user;
        try {
            user = db.query(User.class).eq("id", uid).first();
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        user.setStatus("banned");
        db.commit();
        return user;
    }

    @Override
    public User unbanuser(int uid) {
        User user;
        try {
            user = db.query(User.class).eq("id", uid).first();
        } catch (NoSuchElementException e) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        user.setStatus("normal");
        db.commit();
        return user;
    }

    @Override
    public User updateUser(User user, UserRoles userRoles) {
        User user1 = db.query(User.class).eq("id", user.getId()).first();
        // 管理员可以修改任何用户信息
        if (rbacService.isAdmin(userRoles)) {
            UniversalUtils.updateObj(user1, user);
            db.commit();
        } else if (userRoles.uid == user.getId()) {
            // 用户可以修改自己的信息
            UniversalUtils.updateObj(user1, user);
            db.commit();

        } else {
            throw new Http_403_ForbiddenException("权限不足");
        }
        return user1;
    }
}

