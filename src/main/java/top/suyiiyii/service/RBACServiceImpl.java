package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.RBACRole;
import top.suyiiyii.models.RBACUser;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.orm.core.Session;

import java.util.ArrayList;
import java.util.List;


@Repository
public class RBACServiceImpl implements RBACService {
    private final Session db;

    public RBACServiceImpl(Session db) {
        this.db = db;
    }

    /**
     * 通过uid获得用户的角色信息
     *
     * @param uid
     * @return
     */
    @Override
    public List<String> getRoles(int uid) {
        try {
            List<RBACUser> rbacUser = db.query(RBACUser.class).eq("uid", uid).all();
            List<String> roles = new ArrayList<>();
            for (RBACUser user : rbacUser) {
                roles.add(user.getRole());
            }
            roles.add("guest");
            return roles;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean checkPermission(String role, String permission) {
        List<RBACRole> rbacRoles = db.query(RBACRole.class).eq("role", role).eq("permission", permission).all();
        return rbacRoles.size() > 0;
    }

    @Override
    public boolean checkPermission(List<String> roles, String permission) {
        for (String role : roles) {
            if (checkPermission(role, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过uid检查用户是否有权限
     *
     * @param uid        用户id
     * @param permission 权限
     * @return 是否有权限
     */
    @Override
    public boolean checkPermission(int uid, String permission) {
        List<String> roles = getRoles(uid);
        return checkPermission(roles, permission);
    }

    @Override
    public boolean checkPermission(UserRoles userRoles, String permission) {
        return checkPermission(userRoles.getRoles(), permission);
    }

    @Override
    public void addRolePermission(String role, String permission) {
        RBACRole rbacRole = new RBACRole();
        rbacRole.setRole(role);
        rbacRole.setPermission(permission);
        db.insert(rbacRole);
    }

    @Override
    public void deleteRolePermission(String role, String permission) {
        db.delete(RBACRole.class).eq("role", role).eq("permission", permission).execute();
    }

    @Override
    public void addUserRole(int uid, String role) {
        RBACUser rbacUser = new RBACUser();
        rbacUser.setUid(uid);
        rbacUser.setRole(role);
        db.insert(rbacUser);
    }

    @Override
    public void deleteUserRole(int uid, String role) {
        db.delete(RBACUser.class).eq("uid", uid).eq("role", role).execute();
    }
}
