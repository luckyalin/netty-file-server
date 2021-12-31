package com.voicecomm.upload.util;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.voicecomm.upload.config.FileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * @ClassName FastDFSUtil
 * @Author yulin.li
 * @Date 2021/12/21 17:19
 * @Description FastDFSUtil
 */
@Component
@Slf4j
public class FastDfsUtil {
    private final FastFileStorageClient storageClient;

    private final FileProperties fileProperties;

    public FastDfsUtil(FastFileStorageClient storageClient, FileProperties fileProperties) {
        this.storageClient = storageClient;
        this.fileProperties = fileProperties;
    }

    /**
     * fastdfs文件上传方法
     * @param inputStream  文件输入流
     * @param size 文件大小
     * @param fileName  文件名
     * @return fullPath 返回文件访问地址
     */
    public String upload(InputStream inputStream, Long size, String fileName) {
        log.info("上传文件开始，文件名：" + fileName + "  文件大小：" + size);
        StorePath storePath = storageClient.uploadFile(inputStream, size, fileName, null);
        String fullPath = storePath.getFullPath();
        return fileProperties.getDomain() + "/" + fullPath;
    }
}
