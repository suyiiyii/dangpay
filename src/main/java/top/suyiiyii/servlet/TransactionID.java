package top.suyiiyii.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.Data;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.service.GroupService;
import top.suyiiyii.service.TransactionService;
import top.suyiiyii.service.UploadService;
import top.suyiiyii.service.WalletService;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.servlet.IngressServlet;
import top.suyiiyii.su.validator.Regex;

import java.io.IOException;
import java.io.InputStream;

public class TransactionID {


    private final GroupService groupService;
    private final UserRoles userRoles;
    private final WalletService walletService;
    TransactionService transactionService;
    IngressServlet.SubMethod subMethod;
    UploadService uploadService;

    public TransactionID(TransactionService transactionService, GroupService groupService, UserRoles userRoles, WalletService walletService, IngressServlet.SubMethod subMethod, UploadService uploadService) {
        this.transactionService = transactionService;
        this.groupService = groupService;
        this.userRoles = userRoles;
        this.walletService = walletService;
        this.subMethod = subMethod;
        this.uploadService = uploadService;
    }


    public boolean doPostReimburse(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = null;

        // 获取用户上传的文件
        Part filePart = req.getPart("file");
        String fileName = filePart.getSubmittedFileName();
        InputStream in = filePart.getInputStream();

        // 检查文件
        // 如果文件为空，返回400
        if (in == null) {
            throw new Http_400_BadRequestException("文件不能为空");
        }
        
        // 如果文件超过10m，返回400
        if (filePart.getSize() > 1024 * 1024 * 10) {
            throw new Http_400_BadRequestException("文件大小不能超过10M");
        }
        // 将文件保存到S3
        url = uploadService.uploadFile(fileName, in, userRoles.getUid());

        if (url != null) {
            transactionService.setReimburse(subMethod.getId(), url, userRoles.getUid());
            return true;
        } else {
            throw new Http_400_BadRequestException("文件上传失败");
        }
    }

    @Data
    static class Reimburse {
        @Regex("https?://[\\w./]+")
        String url;
    }
}
