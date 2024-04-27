package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Friend;
import top.suyiiyii.models.User;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class FriendServiceImpl implements FriendService {

    Session db;
    RBACService rbacService;
    UserRoles userRoles;

    public FriendServiceImpl(Session db,
                             @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService,
                             UserRoles userRoles) {
        this.db = db;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
    }

    @Override
    public void addFriend(int uid1, int uid2) {
        // 检查是否存在用户
        if (!db.query(User.class).eq("id", uid2).exists()) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        // 检查是否添加自己
        if (uid1 == uid2) {
            throw new Http_400_BadRequestException("不能添加自己为好友");
        }
        // 检查是否已经是好友
        if (db.query(Friend.class).eq("uid1", uid1).eq("uid2", uid2).exists()) {
            throw new Http_400_BadRequestException("已经是好友");
        }
        // 添加好友
        Friend friend1 = new Friend();
        friend1.setUid1(uid1);
        friend1.setUid2(uid2);
        db.insert(friend1);
        Friend friend2 = new Friend();
        friend2.setUid1(uid2);
        friend2.setUid2(uid1);
        db.insert(friend2);
    }

    @Override
    public void deleteFriend(int uid1, int uid2) {
        // 检查是否存在用户
        if (!db.query(User.class).eq("uid", uid2).exists()) {
            throw new Http_400_BadRequestException("用户不存在");
        }
        db.delete(Friend.class).eq("uid1", uid1).eq("uid2", uid2).execute();
        db.delete(Friend.class).eq("uid1", uid2).eq("uid2", uid1).execute();
    }

    @Override
    public List<FriendDto> getMyFriends(int uid) {
        List<Friend> friends = db.query(Friend.class).eq("uid1", uid).all();
        List<User> users = db.query(User.class).in("id", friends.stream().map(Friend::getUid2).collect(Collectors.toList())).all();
        Map<Integer, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        List<FriendDto> friendDtos = friends.stream().map(friend -> {
            FriendDto friendDto = new FriendDto();
            friendDto.uid = friend.getUid2();
            friendDto.username = userMap.get(friend.getUid2());
            return friendDto;
        }).collect(Collectors.toList());
        // 添加系统消息对话
        FriendDto system = new FriendDto();
        system.uid = -1;
        system.username = "系统消息";
        friendDtos.add(0, system);
        return friendDtos;
    }


}
