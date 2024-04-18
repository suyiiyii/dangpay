package top.suyiiyii.dto;

import lombok.Data;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.Proxy;

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

    public UserRoles(TokenData tokenData,
                     @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService) {
        this.uid = tokenData.uid;
        this.roles = rbacService.getRoles(tokenData.uid);
    }

    public UserRoles() {
    }


}
