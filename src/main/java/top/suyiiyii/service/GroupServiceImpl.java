package top.suyiiyii.service;

import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.GroupModel;
import top.suyiiyii.models.RBACUser;
import top.suyiiyii.models.User;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Repository
public class GroupServiceImpl implements GroupService {
    Session db;
    RBACService rbacService;
    UserRoles userRoles;

    public GroupServiceImpl(Session db,
                            @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService,
                            UserRoles userRoles) {
        this.db = db;
        this.rbacService = rbacService;
        this.userRoles = userRoles;
    }

    /**
     * 创建群组
     *
     * @param uid        用户id
     * @param groupModel 群组信息
     * @return 群组信息
     */
    @Override
    public GroupModel createGroup(int uid, GroupModel groupModel) {
        try {
            db.query(GroupModel.class).eq("name", groupModel.getName()).first();
            throw new Http_400_BadRequestException("群组名已存在");
        } catch (NoSuchElementException ignored) {
        }
        try {
            db.beginTransaction();
            // 创建群组
            groupModel.setStatus("normal");
            groupModel.setHide("false");
            int id = db.insert(groupModel, true);
            // 添加群组管理员
            rbacService.addUserRole(uid, "GroupCreator/g" + id);
            rbacService.addUserRole(uid, "GroupAdmin/g" + id);
            rbacService.addUserRole(uid, "GroupMember/g" + id);
            db.commitTransaction();
            return groupModel;
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 更新群组信息
     */
    @Override
    public void updateGroup(@SubRegion(areaPrefix = "g") int gid, UserRoles userRoles, GroupModel groupModel) {
        GroupModel groupModel1 = db.query(GroupModel.class).eq("id", gid).first();
        if (!groupModel1.getName().equals(groupModel.getName())) {
            try {
                db.query(GroupModel.class).eq("name", groupModel.getName()).first();
                throw new Http_400_BadRequestException("群组名已存在");
            } catch (NoSuchElementException ignored) {
            }
        }
        UniversalUtils.updateObj(groupModel1, groupModel);
        db.commit();
    }

    /**
     * 获取所有群组
     * 用户默认不可见ban状态和隐藏的群组的群组
     *
     * @param isSeeBan 是否可见ban状态的群组
     * @return 群组列表
     */
    @Override
    public List<GroupDto> getAllGroup(boolean isSeeBan) {
        List<GroupModel> groupModels = db.query(GroupModel.class).all();
        if (!isSeeBan) {
            groupModels.removeIf(groupModel -> groupModel.getStatus().equals("ban"));
            groupModels.removeIf(groupModel -> groupModel.getHide().equals("true"));
        }
        List<GroupDto> groupDtos = groupModels.stream().map(groupModel -> {
            GroupDto groupDto = new GroupDto();
            UniversalUtils.updateObj(groupDto, groupModel);
            groupDto.setPepoleCount(String.valueOf(db.query(RBACUser.class).eq("role", "GroupMember/g" + userRoles.getUid()).count()));
            groupDto.setAmIAdmin(rbacService.checkUserRole(userRoles.getUid(), "GroupAdmin/g" + groupModel.getId()));
            RBACUser creator = db.query(RBACUser.class).eq("role", "GroupCreator/g" + groupModel.getId()).first();
            groupDto.setGroupCreatorId(creator.getUid());
            return groupDto;
        }).toList();
        return groupDtos;
    }

    @Override
    public List<GroupDto> getAllGroup() {
        return getAllGroup(false);
    }

    /**
     * 获取用户的所有群组
     *
     * @param uid 用户id
     * @return 群组列表
     */


    @Override
    public List<GroupDto> getMyGroup(int uid) {
        // 先获取用户的所有群组
        List<RBACUser> rbacUsers = db.query(RBACUser.class).eq("uid", uid).fuzzLike("role", "Group").all();
        // 再获取群组的详细信息
        List<GroupModel> groupModels = db.query(GroupModel.class).in("id", List.of(rbacUsers.stream().map(rbacUser -> Integer.parseInt(rbacUser.getRole().split("/")[1].substring(1))).toArray())).all();
        // 封装数据
        List<GroupDto> groupDtos = groupModels.stream().map(groupModel -> {
            GroupDto groupDto = new GroupDto();
            UniversalUtils.updateObj(groupDto, groupModel);
            groupDto.setPepoleCount(String.valueOf(db.query(RBACUser.class).eq("role", "GroupMember/g" + groupModel.getId()).count()));
            groupDto.setAmIAdmin(rbacUsers.stream().anyMatch(rbacUser -> rbacUser.getRole().equals("GroupAdmin/g" + groupModel.getId())));
            RBACUser creator = db.query(RBACUser.class).eq("role", "GroupCreator/g" + groupModel.getId()).first();
            groupDto.setGroupCreatorId(creator.getUid());
            return groupDto;
        }).toList();

        return groupDtos;
    }

    /**
     * 获取群组信息
     * 普通用户默认不可见ban状态的群组
     *
     * @param gid      群组id
     * @param isSeeBan 是否可见ban状态的群组
     * @return 群组信息
     */

    @Override
    public GroupDto getGroup(@SubRegion(areaPrefix = "g") int gid, boolean isSeeBan) {
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        if (groupModel.getStatus().equals("ban") && !isSeeBan) {
            throw new NoSuchElementException("群组不存在");
        }

        GroupDto groupDto = new GroupDto();
        UniversalUtils.updateObj(groupDto, groupModel);
        groupDto.setPepoleCount(String.valueOf(db.query(RBACUser.class).eq("role", "GroupMember/g" + gid).count()));
        groupDto.setAmIAdmin(rbacService.checkUserRole(userRoles.getUid(), "GroupAdmin/g" + gid));
        RBACUser creator = db.query(RBACUser.class).eq("role", "GroupCreator/g" + groupModel.getId()).first();
        groupDto.setGroupCreatorId(creator.getUid());
        if (rbacService.isAdmin(userRoles)) {
            groupDto.setAmIAdmin(true);
        }
        return groupDto;
    }

    @Override
    public GroupDto getGroup(@SubRegion(areaPrefix = "g") int gid) {
        return getGroup(gid, false);
    }

    /**
     * 封禁一个群组
     *
     * @param gid 群组id
     */
    @Override
    public void banGroup(@SubRegion(areaPrefix = "g") int gid) {
        // 判断群组是否已经被封禁
        if (db.query(GroupModel.class).eq("id", gid).eq("status", "ban").exists()) {
            throw new Http_400_BadRequestException("群组已被封禁");
        }
        // 封禁群组
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setStatus("ban");
        db.commit();
    }

    /**
     * 解封一个群组
     *
     * @param gid 群组id
     */
    @Override
    public void unbanGroup(@SubRegion(areaPrefix = "g") int gid) {
        // 判断群组是否已经被封禁
        if (db.query(GroupModel.class).eq("id", gid).eq("status", "normal").exists()) {
            throw new Http_400_BadRequestException("群组状态正常");
        }
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setStatus("normal");
        db.commit();
    }

    /**
     * 设置一个群组为隐藏状态
     *
     * @param gid 群组id
     */

    @Override
    public void hideGroup(@SubRegion(areaPrefix = "g") int gid) {
        // 判断是否已经隐藏
        if (db.query(GroupModel.class).eq("id", gid).eq("hide", "true").exists()) {
            throw new Http_400_BadRequestException("群组已隐藏");
        }
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setHide("true");
        db.commit();
    }

    /**
     * 设置一个群组为显示状态
     *
     * @param gid 群组id
     */

    @Override
    public void unhideGroup(@SubRegion(areaPrefix = "g") int gid) {
        // 判断是否已经隐藏
        if (db.query(GroupModel.class).eq("id", gid).eq("hide", "false").exists()) {
            throw new Http_400_BadRequestException("群组未隐藏");
        }
        GroupModel groupModel = db.query(GroupModel.class).eq("id", gid).first();
        groupModel.setHide("false");
        db.commit();
    }

    /**
     * 用户主动加入群组
     * 只能加入设置为公开并且未被封禁的群组
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void joinGroup(@SubRegion(areaPrefix = "g") int gid, int uid) {
        if (!rbacService.isAdmin(userRoles)) {
            // 判断群组是否存在，是否被封禁，是否隐藏
            if (!db.query(GroupModel.class).eq("id", gid).eq("status", "normal").eq("hide", "false").exists()) {
                throw new Http_400_BadRequestException("群组不存在或不可加入");
            }
        }
        rbacService.addUserRole(uid, "GroupMember/g" + gid);
    }

    /**
     * 用户自己主动退出群组
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void leaveGroup(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            if (db.query(RBACUser.class).eq("uid", uid).eq("role", "GroupCreator/g" + gid).exists()) {
                throw new Http_400_BadRequestException("群主不能退出");
            }
            rbacService.deleteUserRole(uid, "GroupMember/g" + gid);
            rbacService.deleteUserRole(uid, "GroupAdmin/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 删除一个群组成员
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void kickGroupMember(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            if (!rbacService.checkUserRole(uid, "GroupMember/g" + gid)) {
                throw new Http_400_BadRequestException("用户不是群组成员");
            }
            if (uid == userRoles.getUid()) {
                throw new Http_400_BadRequestException("不能踢自己");
            }
            // 群主能踢管理员和普通成员，管理员只能踢普通成员
            if (rbacService.checkUserRole(uid, "GroupCreator/g" + gid)) {
                throw new Http_400_BadRequestException("不能踢群主");
            }
            if (!rbacService.checkUserRole(userRoles, "GroupCreator/g" + gid) && rbacService.checkUserRole(uid, "GroupAdmin/g" + gid)) {
                throw new Http_400_BadRequestException("只有群主能踢管理员");
            }
            rbacService.deleteUserRole(uid, "GroupMember/g" + gid);
            rbacService.deleteUserRole(uid, "GroupAdmin/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 获取群组的所有成员
     *
     * @param gid 群组id
     * @return 成员列表
     */
    @Override
    public List<MemberDto> getGroupMembers(@SubRegion(areaPrefix = "g") int gid) {
        // 先获取群组的所有成员id
        List<RBACUser> rbacUsers = db.query(RBACUser.class).eq("role", "GroupMember/g" + gid).all();
        // 获取管理员的id
        List<RBACUser> rbacAdmins = db.query(RBACUser.class).eq("role", "GroupAdmin/g" + gid).all();
        // 获取群主的id
        RBACUser creator = db.query(RBACUser.class).eq("role", "GroupCreator/g" + gid).first();
        // 再获取成员的名字
        List<User> users = db.query(User.class).in("id", List.of(rbacUsers.stream().map(RBACUser::getUid).toArray())).all();
        Map<Integer, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        //构造返回数据
        List<MemberDto> memberDtos = rbacUsers.stream().map(rbacUser -> {
            MemberDto memberDto = new MemberDto();
            memberDto.setId(rbacUser.getUid());
            memberDto.setName(userMap.get(rbacUser.getUid()));
            if (creator.getUid() == rbacUser.getUid()) {
                memberDto.setRole("creator");
            } else if (rbacAdmins.stream().anyMatch(rbacUser1 -> rbacUser1.getUid() == rbacUser.getUid())) {
                memberDto.setRole("admin");
            } else {
                memberDto.setRole("member");
            }
            return memberDto;
        }).toList();
        return memberDtos;
    }

    /**
     * 邀请用户加入群组
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void inviteUser(@SubRegion(areaPrefix = "g") int gid, int uid) {
        rbacService.addUserRole(uid, "GroupMember/g" + gid);
    }

    /**
     * 添加群组管理员
     * 成为管理员之前必须是群组成员
     *
     * @param gid 群组id
     * @param uid 用户id
     */
    @Override
    public void addAdmin(@SubRegion(areaPrefix = "g") int gid, int uid) {
        // 判断是不是群组成员
        if (!rbacService.checkUserRole(uid, "GroupMember/g" + gid)) {
            throw new Http_400_BadRequestException("用户不是群组成员");
        }
        rbacService.addUserRole(uid, "GroupAdmin/g" + gid);
    }

    /**
     * 转让群主
     * 只有群主能转让群主
     */
    @Override
    public void transferGroupCreator(@SubRegion(areaPrefix = "g") int gid, int uid) {
        try {
            db.beginTransaction();
            if (!rbacService.checkUserRole(userRoles, "GroupCreator/g" + gid)) {
                throw new Http_400_BadRequestException("没有权限");
            }
            if (!rbacService.checkUserRole(uid, "GroupAdmin/g" + gid)) {
                throw new Http_400_BadRequestException("用户不是群组管理员");
            }
            rbacService.deleteUserRole(userRoles.getUid(), "GroupCreator/g" + gid);
            rbacService.addUserRole(uid, "GroupCreator/g" + gid);
            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw e;
        }
    }

    /**
     * 销毁群组
     * 只有群主能销毁群组
     * 销毁之前需要检查是否有其他群组成员
     * 并且检查是否有子钱包非空
     */

    @Override
    @Proxy(isTransaction = true)
    public void destroyGroup(@SubRegion(areaPrefix = "g") int gid) {
        // 检查群内是否有其他成员
        if (db.query(RBACUser.class).eq("role", "GroupMember/g" + gid).count() > 1) {
            throw new Http_400_BadRequestException("群组内还有其他成员，请先清理其他成员后再试");
        }
        // 检查主钱包是否为空
        Wallet mainWallet = new Wallet();
        mainWallet.setId(-1);
        try {
            mainWallet = db.query(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").first();
            if (mainWallet.getAmount() != 0 || mainWallet.getAmountInFrozen() != 0) {
                throw new Http_400_BadRequestException("群组主钱包还有余额或未完成的订单，请先转移后再试");
            }
        } catch (NoSuchElementException e) {
        }
        // 检查是否有子钱包非空
        try {
            List<Wallet> subWallets = db.query(Wallet.class).eq("father_wallet_id", mainWallet.getId()).all();
            if (subWallets.stream().anyMatch(wallet -> wallet.getAmount() != 0 || wallet.getAmountInFrozen() != 0)) {
                throw new Http_400_BadRequestException("群组内还有子钱包有余额或未完成的订单，请先转移后再试");
            }
        } catch (NoSuchElementException e) {
        }
        // 删除群组
        db.delete(GroupModel.class).eq("id", gid).execute();
        // 删除群组的子钱包
        db.delete(Wallet.class).eq("father_wallet_id", mainWallet.getId()).execute();
        // 删除群组的主钱包
        db.delete(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").execute();
        // 删除群组的权限
        db.delete(RBACUser.class).eq("role", "GroupCreator/g" + gid).execute();
        db.delete(RBACUser.class).eq("role", "GroupAdmin/g" + gid).execute();
        db.delete(RBACUser.class).eq("role", "GroupMember/g" + gid).execute();
    }
}
