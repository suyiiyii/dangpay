package top.suyiiyii.dao;


/**
 * 平台向第三方平台发起交易请求，会返回一个code，下次发送请求时需要携带这个code
 * 同时code有过期时间，过期后无法使用
 * 而交易的过程是不连续的，所以无论是发起交易请求还是验证交易请求，都需要记录交易的基本信息，记录code
 * 因此提出TransactionDao，在Transaction创建之前记录交易的基本信息
 */
public class TransactionDao {
    //TODO


}
