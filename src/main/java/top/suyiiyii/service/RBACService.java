package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.su.IOC.RBACAuthorization;

import java.util.List;

@RBACAuthorization
public interface RBACService {
    List<String> getRoles(int uid);

    boolean checkPermission(String role, String permission);

    boolean checkPermission(List<String> roles, String permission);

    boolean checkPermission(int uid, String permission);

    boolean checkPermission(UserRoles userRoles, String permission);
}

