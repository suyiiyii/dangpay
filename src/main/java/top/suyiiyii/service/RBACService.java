package top.suyiiyii.service;

import top.suyiiyii.models.RBACUser;
import top.suyiiyii.su.orm.core.Session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RBACService {
    private Session db;

    public RBACService(Session db) {
        this.db = db;
    }

    /**
     * 通过
     * @param uid
     * @return
     */
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
