package top.suyiiyii.service;

import top.suyiiyii.models.RBACUser;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.orm.core.Session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@Repository
public class RBACServiceImpl implements RBACService {
    private Session db;

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
            return roles;
        } catch (SQLException | RuntimeException e) {
            return new ArrayList<>();
        }
    }
}
