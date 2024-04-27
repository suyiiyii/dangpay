package top.suyiiyii.service;

import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Transaction;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.su.ConfigManger;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.IOC.SubRegion;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;

@Slf4j
@Repository
public class WalletServiceImpl implements WalletService {
    Session db;
    RBACService rbacService;
    ConfigManger configManger;
    UserService userService;
    GroupService groupService;

    public WalletServiceImpl(Session db,
                             @Proxy(isNeedAuthorization = false) RBACService rbacService,
                             @Proxy(isNeedAuthorization = false) UserService userService,
                             ConfigManger configManger,
                             @Proxy(isNeedAuthorization = false) GroupService groupService) {
        this.db = db;
        this.userService = userService;
        this.rbacService = rbacService;
        this.configManger = configManger;
        this.groupService = groupService;
    }

    /**
     * 创建个人钱包
     *
     * @param uid 用户id
     */
    @Override
    @Proxy(isTransaction = true)
    public void createPersonalWallet(int uid) {
        // 检查是否已经有钱包
        if (db.query(Wallet.class).eq("owner_id", uid).eq("owner_type", "user").eq("is_sub_wallet", 0).exists()) {
            throw new Http_400_BadRequestException("钱包已存在");
        }
        // 创建个人钱包
        Wallet wallet = new Wallet();
        wallet.setName(userService.getUser(uid, new UserRoles(-1)).getUsername() + " 的个人钱包");
        wallet.setAmount(0);
        wallet.setAmountInFrozen(0);
        wallet.setOwnerType("user");
        wallet.setOwnerId(uid);
        wallet.setLastUpdate(UniversalUtils.getNow());
        wallet.setIsSubWallet(0);
        wallet.setFatherWalletId(0);
        wallet.setStatus("normal");
        int id = db.insert(wallet, true);
        // 分配个人钱包权限
        rbacService.addUserRole(uid, "WalletAdmin/w" + id);
    }

    @Override
    @Proxy(isTransaction = true)
    public void createGroupWallet(@SubRegion(areaPrefix = "g") int gid) {
        groupService.checkGroupStatus(gid);
        // 检查是否已经有钱包
        if (db.query(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").exists()) {
            throw new Http_400_BadRequestException("钱包已存在");
        }
        // 创建群组钱包
        Wallet wallet = new Wallet();
        wallet.setName("群组 " + gid + " 的钱包");
        wallet.setAmount(0);
        wallet.setAmountInFrozen(0);
        wallet.setOwnerType("group");
        wallet.setOwnerId(gid);
        wallet.setLastUpdate(UniversalUtils.getNow());
        wallet.setIsSubWallet(0);
        wallet.setFatherWalletId(0);
        wallet.setStatus("normal");
        int id = db.insert(wallet, true);
        // 给群组管理员分配钱包权限
        List<Integer> admins = rbacService.getUserByRole("GroupAdmin/g" + gid);
        for (int uid : admins) {
            rbacService.addUserRole(uid, "WalletAdmin/w" + id);
        }
    }


    /**
     * deprecated
     * 创建子账户
     * 一个父账户可以有多个子账户
     *
     * @param fatherWalletId 父账户id
     * @param uid            子账户拥有者id
     * @return 子账户
     */
    @Override
    public Wallet createSubWallet(@SubRegion(areaPrefix = "w") int fatherWalletId, int uid) {
        // 检查是否已经有子钱包
        if (db.query(Wallet.class).eq("owner_id", uid).eq("owner_type", "user").eq("father_wallet_id", fatherWalletId).exists()) {
            throw new Http_400_BadRequestException("子账户已存在");
        }
        // 检查父账户是否存在
        Wallet fatherWallet = db.query(Wallet.class).eq("id", fatherWalletId).first();
        if (fatherWallet == null) {
            throw new IllegalArgumentException("父账户不存在");
        }
        Wallet wallet = new Wallet();
        // 获取父账户的群组的名称

        wallet.setName(fatherWallet.getName() + " 分配给 " + userService.getUser(uid, new UserRoles(-1)).getUsername() + " 的子账户");
        wallet.setAmount(0);
        wallet.setAmountInFrozen(0);
        wallet.setOwnerType("user");
        wallet.setOwnerId(uid);
        wallet.setLastUpdate(UniversalUtils.getNow());
        wallet.setIsSubWallet(1);
        wallet.setFatherWalletId(fatherWalletId);
        wallet.setStatus("normal");
        int id = db.insert(wallet);
        // 给子账户拥有者分配钱包权限
        rbacService.addUserRole(uid, "WalletAdmin/w" + id);
        return wallet;
    }

    /**
     * 创建群组的子钱包
     */
    @Override
    @Proxy(isTransaction = true)
    public void createGroupSubWallet(@SubRegion(areaPrefix = "g") int gid, int uid) {
        groupService.checkGroupStatus(gid);
        // 获取群组的主账户id
        Wallet groupWallet = db.query(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").first();
        if (groupWallet == null) {
            throw new IllegalArgumentException("群组钱包不存在");
        }
        // 检查是否已经有子钱包
        if (db.query(Wallet.class).eq("owner_id", uid).eq("owner_type", "user").eq("father_wallet_id", groupWallet.getId()).exists()) {
            throw new Http_400_BadRequestException("子账户已存在");
        }
        Wallet wallet = new Wallet();
        // 获取父账户的群组的名称
        wallet.setName(groupWallet.getName() + " 分配给 " + userService.getUser(uid, new UserRoles(-1)).getUsername() + " 的子账户");
        wallet.setAmount(0);
        wallet.setAmountInFrozen(0);
        wallet.setOwnerType("user");
        wallet.setOwnerId(uid);
        wallet.setLastUpdate(UniversalUtils.getNow());
        wallet.setIsSubWallet(1);
        wallet.setFatherWalletId(groupWallet.getId());
        wallet.setStatus("normal");
        int id = db.insert(wallet, true);
        // 给子账户拥有者分配钱包权限
        rbacService.addUserRole(uid, "WalletAdmin/w" + id);
        // 给管理员分配观察者权限
        List<Integer> admins = rbacService.getUserByRole("GroupAdmin/g" + gid);
        for (int admin : admins) {
            rbacService.addUserRole(admin, "WalletObserver/w" + id);
        }
    }

    @Override
    public Wallet getWallet(@SubRegion(areaPrefix = "w") int id) {
        return db.query(Wallet.class).eq("id", id).first();
    }

    @Override
    public List<Wallet> getMyWallets(int uid) {
        return db.query(Wallet.class).eq("owner_id", uid).eq("owner_type", "user").all();
    }

    @Override
    public List<Wallet> getGroupWallets(@SubRegion(areaPrefix = "g") int gid) {
        return db.query(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").all();
    }

    @Override
    public List<Wallet> getGroupSubWallets(@SubRegion(areaPrefix = "g") int gid) {
        // 获取群组的主账户id
        Wallet groupWallet = db.query(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").first();
        if (groupWallet == null) {
            throw new IllegalArgumentException("群组钱包不存在");
        }
        return db.query(Wallet.class).eq("father_wallet_id", groupWallet.getId()).all();
    }

    /**
     * 将父钱包里面的钱分配到子钱包
     *
     * @param fatherWalletId 父账户id
     * @param subWalletId    子账户id
     * @param amount         金额
     */
    @Override
    @Proxy(isTransaction = true)
    public void allocate(@SubRegion(areaPrefix = "w") int fatherWalletId, int subWalletId, int amount) {
        // 检查父子账户关系
        Wallet fatherWallet = db.query(Wallet.class).eq("id", fatherWalletId).first();
        Wallet subWallet = db.query(Wallet.class).eq("id", subWalletId).first();
        if (fatherWallet.getId() != subWallet.getFatherWalletId()) {
            throw new IllegalArgumentException("父子账户关系错误");
        }
        // 检查余额
        if (fatherWallet.getAmount() < amount) {
            throw new IllegalArgumentException("余额不足");
        }
        // 检查账户状态
        checkWalletStatus(fatherWalletId);
        checkWalletStatus(subWalletId);
        // 检查分配之后钱包金额会不会是负数
        if (subWallet.getAmount() + amount < 0) {
            throw new IllegalArgumentException("分配之后子账户余额将为负数");
        }
        if (fatherWallet.getAmount() - amount < 0) {
            throw new IllegalArgumentException("分配之后父账户余额将为负数");
        }

        // 转账，减少父账户资金，增加子账户资金
        fatherWallet.setAmount(fatherWallet.getAmount() - amount);
        subWallet.setAmount(subWallet.getAmount() + amount);
        // 记录交易（父钱包付款）
        String platform = configManger.get("PLATFORM_NAME");
        String description = "父账户" + fatherWalletId + "向子账户" + subWalletId + "转账" + amount + "元";
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType("allocate");
        transaction.setStatus("finish");
        transaction.setCreateTime(UniversalUtils.getNow());
        transaction.setLastUpdate(UniversalUtils.getNow());
        transaction.setPlatform(platform);
        transaction.setDescription(description);
        transaction.setWalletId(fatherWalletId);
        transaction.setRelateUserId(subWallet.getOwnerId());
        transaction.setReimburse("N/A");
        db.insert(transaction);
        // 记录交易（子钱包收款）
        transaction.setWalletId(subWalletId);
        transaction.setAmount(-amount);
        db.insert(transaction);
        db.commit();
    }

    /**
     * 将群组钱包的钱分配到子账户
     */

    @Override
    @Proxy(isTransaction = true)
    public void allocateGroupWallet(@SubRegion(areaPrefix = "g") int gid, int subWalletId, int amount) {
        groupService.checkGroupStatus(gid);
        // 获取群组的主账户id
        Wallet groupWallet = db.query(Wallet.class).eq("owner_id", gid).eq("owner_type", "group").first();
        if (groupWallet == null) {
            throw new IllegalArgumentException("群组钱包不存在");
        }
        allocate(groupWallet.getId(), subWalletId, amount);
    }


    @Override
    public List<Transaction> getWalletTransactions(@SubRegion(areaPrefix = "w") int wid) {
        return db.query(Transaction.class).eq("wallet_id", wid).all();
    }

    @Override
    public void checkWalletStatus(int wid) {
        Wallet wallet = db.query(Wallet.class).eq("id", wid).first();
        if (!wallet.getStatus().equals("normal")) {
            log.error("钱包状态异常，钱包id：" + wallet.getId() + "，状态：" + wallet.getStatus());
            throw new Http_400_BadRequestException("您的钱包状态异常，请联系管理员或稍后再试");
        }
    }
}
