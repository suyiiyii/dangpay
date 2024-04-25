package top.suyiiyii.service;

import top.suyiiyii.models.Transaction;
import top.suyiiyii.models.Wallet;
import top.suyiiyii.su.IOC.Proxy;
import top.suyiiyii.su.IOC.SubRegion;

import java.util.List;

@Proxy
public interface WalletService {
    @Proxy(isTransaction = true)
    void createPersonalWallet(int uid);

    void createGroupWallet(@SubRegion(areaPrefix = "g") int gid);

    Wallet createSubWallet(@SubRegion(areaPrefix = "w") int fatherWalletId, int uid);

    @Proxy(isTransaction = true)
    void createGroupSubWallet(@SubRegion(areaPrefix = "g") int gid, int uid);

    Wallet getWallet(@SubRegion(areaPrefix = "w") int id);

    List<Wallet> getMyWallets(int uid);

    List<Wallet> getGroupWallets(@SubRegion(areaPrefix = "g") int gid);

    List<Wallet> getGroupSubWallets(@SubRegion(areaPrefix = "g") int gid);

    @Proxy(isTransaction = true)
    void allocate(@SubRegion(areaPrefix = "w") int fatherWalletId, int subWalletId, int amount);

    void allocateGroupWallet(@SubRegion(areaPrefix = "g") int gid, int subWalletId, int amount);

    List<Transaction> getWalletTransactions(@SubRegion(areaPrefix = "w") int wid);

    void checkWalletStatus(int wid);
}
