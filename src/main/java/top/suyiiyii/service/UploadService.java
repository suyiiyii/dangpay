package top.suyiiyii.service;

import java.io.InputStream;

public interface UploadService {
    String uploadAvatar(String filename, InputStream in, int uid);
}
