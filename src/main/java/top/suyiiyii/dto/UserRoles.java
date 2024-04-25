package top.suyiiyii.dto;

import lombok.Data;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.service.RBACServiceImpl;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;


/**
 * 用户角色信息
 * 这里写的很乱，没有分清楚这到底是一个数据类还是一个用于注入的对象
 */
@Data
//@Proxy(isNeedAuthorization = false)
public class UserRoles {
    public int uid;
    public List<String> roles;

    public UserRoles(int uid, Session db) {
        this.uid = uid;
        RBACService rbacService = new RBACServiceImpl(db);
        this.roles = rbacService.getRoles(uid);
    }

    public UserRoles(TokenData tokenData,
                     @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService) {
        this.uid = tokenData.uid;
        this.roles = rbacService.getRoles(tokenData.uid);
    }

    public UserRoles() {
    }

    public UserRoles(int uid) {
        this.uid = uid;
    }
}
