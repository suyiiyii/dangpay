package top.suyiiyii.dto;

import lombok.Data;
import top.suyiiyii.service.RBACService;
import top.suyiiyii.su.IOC.RBACAuthorization;

import java.util.List;

@Data
@RBACAuthorization(isNeedAuthorization = false)
public class UserRoles {
    public int uid;
    public List<String> roles;

    public UserRoles(TokenData tokenData, RBACService rbacService) {
        this.uid = tokenData.uid;
        this.roles = rbacService.getRoles(tokenData.uid);
    }
}
