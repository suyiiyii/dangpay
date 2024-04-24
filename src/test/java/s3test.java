import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import top.suyiiyii.su.ConfigManger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
public class s3test {
    ConfigManger configManger = new ConfigManger("application.yaml");
    private final String endpoint = configManger.get("S3_ENDPOINT");
    private final String accessKey = configManger.get("S3_ACCESS_KEY");
    private final String secretKey = configManger.get("S3_SECRET_KEY");
    private AmazonS3 s3client;

    public static void main(String[] args) throws FileNotFoundException {
        s3test s3test = new s3test();
        try {
            s3test.init();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // 检查 bucket 是否存在
        System.out.println(s3test.bucketExists("buk"));
        // 检查文件是否存在
        System.out.println(s3test.existObject("buk", "系统.png"));
        // 下载文件
        Path projectRoot = Paths.get("");
        log.info("projectRoot: {}", projectRoot.toAbsolutePath().toString());
        try (OutputStream out = new FileOutputStream("系统.png")) {
            s3test.download("buk", "系统.png", out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 上传文件
        InputStream in = new FileInputStream("系统.png");
        try {
            s3test.upload("buk", "系统2.png", in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        in = new FileInputStream("系统.png");

        String newFileName = s3test.uploadFile(in);
        log.info(newFileName);
    }

    public void init() throws MalformedURLException {
        URL endpointUrl = new URL(endpoint);
        String protocol = endpointUrl.getProtocol();
        int port = endpointUrl.getPort() == -1 ? endpointUrl.getDefaultPort() : endpointUrl.getPort();
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setSignerOverride("S3SignerType");
        clientConfig.setProtocol(Protocol.valueOf(protocol.toUpperCase()));
        // 禁用证书检查，避免https自签证书校验失败
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        // 屏蔽 AWS 的 MD5 校验，避免校验导致的下载抛出异常问题
        System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", "true");
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        // 创建 S3Client 实例
        AmazonS3 s3client = new AmazonS3Client(awsCredentials, clientConfig);
        s3client.setEndpoint(endpointUrl.getHost() + ":" + port);
        s3client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
        this.s3client = s3client;
    }

    public boolean bucketExists(String bucket) {
        try {
            return s3client.doesBucketExist(bucket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean existObject(String bucket, String objectId) {
        try {
            return s3client.doesObjectExist(bucket, objectId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public InputStream download(String bucket, String objectId) {
        try {
            S3Object o = s3client.getObject(bucket, objectId);
            return o.getObjectContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void download(String bucket, String objectId, OutputStream out) {
        S3Object o = s3client.getObject(bucket, objectId);
        try (InputStream in = o.getObjectContent()) {
            IOUtils.copyLarge(in, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void upload(String bucket, String objectId, InputStream input) {
        try {
            // 创建文件上传的元数据
            ObjectMetadata meta = new ObjectMetadata();
            // 设置文件上传长度
            meta.setContentLength(input.available());
            // 上传
            s3client.putObject(bucket, objectId, input, meta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String uploadFile(InputStream input) {
        String objectID = UUID.randomUUID().toString();
        String buket = configManger.get("S3_BUKET_NAME");
        upload(buket, objectID, input);
        return endpoint + "/" + buket + "/" + objectID;
    }
}
