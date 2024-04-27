package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.Proxy;

import java.util.List;

@Proxy
public interface UserService {

    boolean checkPassword(String password, int uid);

    String login(String username, String password);

    void changePassword(UserRoles userRoles, int uid, String oldPassword, String newPassword);

    User getUser(int uid, UserRoles userRoles);

    List<User> getUsers(UserRoles userRoles);

    @Proxy(isTransaction = true)
    User register(String username, String password, String phone, String email);

    User banUser(int uid);

    User unbanuser(int uid);

    User updateUser(User user, UserRoles userRoles);
}
