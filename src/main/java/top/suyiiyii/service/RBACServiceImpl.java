package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.RBACRole;
import top.suyiiyii.models.RBACUser;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.orm.core.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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
        // 如果权限是带分区的，需要带有同样分区的角色才能通过
        if (permission.contains("/")) {
            String subRegion = permission.split("/")[1];
            String[] splits = role.split("/");
            if (splits.length < 2 || !splits[1].equals(subRegion)) {
                return false;
            }
            // RBACRole 存的是不带分区的权限，所以需要去掉分区
            permission = permission.split("/")[0];
            role = role.split("/")[0];
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

    @Override
    public boolean checkUserPermission(int uid, String permission) {
        List<String> roles = getRoles(uid);
        return checkPermission(roles, permission);
    }


    @Override
    public boolean checkUserPermission(UserRoles userRoles, String permission) {
        List<String> roles = userRoles.getRoles();
        return checkPermission(roles, permission);
    }

    @Override
    public void addRolePermission(String role, String permission) {
        // 检查待添加的角色权限是否存在
        if (db.query(RBACRole.class).eq("role", role).eq("permission", permission).exists()) {
            return;
        }
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
        // 检查待添加的用户角色是否存在
        if (db.query(RBACUser.class).eq("uid", uid).eq("role", role).exists()) {
            return;
        }
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

    /**
     * 获得具有某个角色的用户
     *
     * @param role 角色（可以带有分组）
     * @return 用户id列表
     */
    @Override
    public List<Integer> getUserByRole(String role) {
        List<RBACUser> rbacUsers = db.query(RBACUser.class).eq("role", role).all();
        List<Integer> uids = rbacUsers.stream().map(RBACUser::getUid).collect(Collectors.toList());
        return uids;
    }

}
