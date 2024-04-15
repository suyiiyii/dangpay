package top.suyiiyii.service;

import top.suyiiyii.models.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface UserService {

    String login(String username, String password);

    User getUser(int uid);

    User register(String username, String password, String phone);
}
