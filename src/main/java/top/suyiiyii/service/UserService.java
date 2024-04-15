package top.suyiiyii.service;

import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.RBACAuthorization;

@RBACAuthorization
public interface UserService {

    String login(String username, String password);

    User getUser(int uid);

    User register(String username, String password, String phone);

    User banUser(int uid);

    User unbanUser(int uid);
}
