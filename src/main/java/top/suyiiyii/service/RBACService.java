package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.su.IOC.Proxy;

import java.util.List;

@Proxy
public interface RBACService {
    List<String> getRoles(int uid);

    boolean checkPermission(String role, String permission);

    boolean checkPermission(List<String> roles, String permission);

    boolean checkUserPermission(int uid, String permission);

    boolean checkUserPermission(UserRoles userRoles, String permission);

    boolean checkUserRole(UserRoles userRoles, String role);

    boolean checkUserRole(int uid, String role);

    void addRolePermission(String role, String permission);

    void deleteRolePermission(String role, String permission);

    void addUserRole(int uid, String role);

    void deleteUserRole(int uid, String role);

    boolean isAdmin(UserRoles userRoles);

    boolean isAdmin(List<String> roles);

    List<Integer> getUserByRole(String role);

}

