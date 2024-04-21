import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.UniversalUtils;

@Slf4j
public class RSAtest {
    public static void main(String[] args) throws Exception {

        String key = "4234423324";
        String data = "123456";
        String ciphertext = UniversalUtils.encrypt(data, key);
        log.info("加密结果: {}", ciphertext);

        String plaintext = UniversalUtils.decrypt(ciphertext, key);
        log.info("解密结果: {}", plaintext);



        String publicKey = """
                ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCnBvFImdvtyQZ8jarsphx51SO0glGZPM6soLTHXDH/wJhFkWClFnpRhQYicqExBRXkNa7V1fiOPxQxCWPGahh7CJdUNYM2H3Mh+DC3xaL/X2Crd0JJyZnA64b0Hc0fG5KnjLpEPVfGivGvLUOJnYbcCRWWaZToKlpxLWRKuw7bTuP0ENnNUwrAa+naRpGDYwKX5tP+5rRJGvjnPouMbROcApryKOMmbCEc5hQadWBNquezKSzzoCf0TTXLH6617njlw5KC5qq0j34omzVxPaz/AeC1jUui8JxzGGAkgs2OGJ0eNxPOm3HQijSKgJ+q/PJRP4eFgt3PTIHWiP1HtxljsQ6Sy0s6CR0RjOJBKTNdcnz3yO35TyEpQx77xAk+7aEal6fivJgMUr01T4OhbLJHuxrjIqtyQPSj667fLyYT2pHOq/M6SNtzvCAGii/KVRvMTdy8rL0uk8A7Uu/p9Ph29P7wOVNAi4vp8NsngjWdYFQz6uaQDRnrmnZwCTJ3xXc= suyiiyii@k3s-ArgoCD
                """;


        String privateKey = """
                """;
        log.info("开始rsa签名测试");
        String sign = UniversalUtils.rsaSign(data, privateKey);
        log.info("签名结果: {}", sign);


        boolean verify = UniversalUtils.rsaVerify(data, sign, publicKey);
        log.info("验签结果: {}", verify);

    }


}
