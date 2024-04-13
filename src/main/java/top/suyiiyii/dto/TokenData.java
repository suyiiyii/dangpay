package top.suyiiyii.dto;

/**
 * TokenData
 * 用于存储token中的数据
 *
 * @author suyiiyii
 */
public class TokenData {
    public int uid;
    public String role;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
