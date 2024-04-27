package top.suyiiyii.service;

import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.S3Client;

import java.io.InputStream;

@Repository
public class UploadServiceImpl implements UploadService {
    private static S3Client s3Client;

    public UploadServiceImpl(S3Client s3Client) {
        UploadServiceImpl.s3Client = s3Client;
    }

    @Override
    public String uploadAvatar(String filename, InputStream in, int uid) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return s3Client.uploadFile(in, uid + ext);
    }

    @Override
    public String uploadFile(String filename, InputStream in, int uid) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return s3Client.uploadFile(in, uid + ext);
    }
}
