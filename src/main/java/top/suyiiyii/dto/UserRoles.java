package top.suyiiyii.dto;

import lombok.Data;
import top.suyiiyii.service.RBACService;

import java.util.List;

@Data
public class UserRoles {
    public int uid;
    public List<String> roles;

    public UserRoles(TokenData tokenData, RBACService rbacService) {
        this.uid = tokenData.uid;
        this.roles = rbacService.getRoles(tokenData.uid);
    }
}
