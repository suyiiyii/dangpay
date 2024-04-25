package top.suyiiyii.service;

import top.suyiiyii.su.S3Client;

import java.io.InputStream;

public class UploadService {
    private static S3Client s3Client;

    public UploadService(S3Client s3Client) {
        UploadService.s3Client = s3Client;
    }

    public String uploadAvatar(String filename, InputStream in, int uid) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return s3Client.uploadFile(in, uid + ext);

    }
}
