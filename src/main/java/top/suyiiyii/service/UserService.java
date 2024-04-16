package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.RBACAuthorization;

import java.util.List;

@RBACAuthorization
public interface UserService {

    String login(String username, String password);

    User getUser(int uid);

    List<User> getUsers(UserRoles userRoles);

    User register(String username, String password, String phone);

    User banUser(int uid);

    User unbanuser(int uid);

    User unbanUser(int uid);
}
