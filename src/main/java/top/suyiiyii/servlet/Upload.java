package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.UploadService;
import top.suyiiyii.su.exception.Http_400_BadRequestException;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件上传servlet
 * 接收前端上传的文件，然后转存到S3，最后返回文件的URL
 */
public class Upload {
    private final UserRoles userRoles;
    private final UploadService uploadService;

    public Upload(UserRoles userRoles, UploadService uploadService) {
        this.userRoles = userRoles;
        this.uploadService = uploadService;
    }

    public String doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            // 获取用户上传的文件
            Part filePart = req.getPart("file");
            String fileName = filePart.getSubmittedFileName();
            InputStream in = filePart.getInputStream();

            // 检查文件
            // 如果文件为空，返回400
            if (in == null) {
                throw new Http_400_BadRequestException("文件不能为空");
            }
            // 如果文件超过1m，返回400
            if (filePart.getSize() > 1024 * 1024) {
                throw new Http_400_BadRequestException("文件大小不能超过1M");
            }
            // 如果文件扩展名不是png或jpg，返回400
            if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg")) {
                throw new Http_400_BadRequestException("只能上传png或jpg文件");
            }

            // 将文件保存到S3
            return uploadService.uploadAvatar(fileName, in, userRoles.getUid());

        } catch (IOException | ServletException e) {
            throw new Http_400_BadRequestException("文件上传失败");
        }

    }
}
