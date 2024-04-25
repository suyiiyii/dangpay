package top.suyiiyii.service;

import lombok.Data;
import top.suyiiyii.su.IOC.Proxy;

import java.util.List;

@Proxy(isNeedAuthorization = false)
public interface FriendService {
    void addFriend(int uid1, int uid2);

    void deleteFriend(int uid1, int uid2);

    List<FriendDto> getMyFriends(int uid);

    @Data
    class FriendDto {
        int uid;
        String username;
    }
}
