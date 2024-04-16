package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.RBACRole;
import top.suyiiyii.models.RBACUser;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.orm.core.Session;

import java.util.ArrayList;
import java.util.List;


/**
 * RBAC权限控制服务
 * <p>
 * RBAC 权限设计
 * 类名 + 方法名 （首字母大写）
 * <p>
 * RBAC 群组身份设计
 * GroupMember/{groupId} 群组成员
 * GroupAdmin/{groupId}  群组管理员
 * 如果用户调用的对应群组id和角色id一致，则该角色生效
 */

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

    /**
     * 检查某个角色有没有某个权限
     *
     * @param role
     * @param permission
     * @return
     */
    @Override
    public boolean checkPermission(String role, String permission) {
        if ("superadmin".equals(role)) {
            return true;
        }
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
     * @param subId      子id
     * @return 是否有权限
     */
    @Override
    public boolean checkUserPermission(int uid, String permission, int subId) {
//        List<String> roles = getRoles(uid);
//        UserRoles userRoles = new UserRoles();
//        userRoles.setUid(uid);
//        userRoles.setRoles(roles);
//        return checkUserPermission(userRoles, permission, subId);
        return false;
    }

    @Override
    public boolean checkUserPermission(UserRoles userRoles, String permission, int subId) {
        List<String> roles = userRoles.getRoles();
        if (subId != -1) {
            // 过滤掉不是当前subId的角色
            roles = roles.stream().filter(role -> {
                if (role.contains("/")) {
                    String[] split = role.split("/");
                    return split[1].equals(String.valueOf(subId));
                }
                return true;
            }).map(role -> {
                if (role.contains("/")) {
                    String[] split = role.split("/");
                    return split[0];
                }
                return role;
            }).toList();
        }
        return checkPermission(roles, permission);
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

    @Override
    public boolean isAdmin(UserRoles userRoles) {
        return isAdmin(userRoles.getRoles());
    }

    @Override
    public boolean isAdmin(List<String> roles) {
        return roles.contains("admin") || roles.contains("superadmin");
    }
}
