package top.suyiiyii.service;

import top.suyiiyii.su.IOC.Proxy;

import java.io.InputStream;

@Proxy(isNeedAuthorization = true)
public interface UploadService {
    String uploadAvatar(String filename, InputStream in, int uid);

    String uploadFile(String filename, InputStream in, int uid);
}
