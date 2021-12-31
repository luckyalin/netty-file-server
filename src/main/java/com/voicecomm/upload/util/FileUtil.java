package com.voicecomm.upload.util;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.voicecomm.upload.config.FileProperties;
import com.voicecomm.upload.constants.FileSizeConstant;
import com.voicecomm.upload.constants.FileTypeConstant;
import com.voicecomm.upload.exception.FileUploadException;
import com.voicecomm.upload.repository.FileUploadRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName FileUtil
 * @Author yulin.li
 * @Date 2021/12/21 17:19
 * @Description FileUtil
 * 文件操作工具类 包含：
 * 1.文件上传到fastdfs
 * 2.文件上传到windows
 * 3.包含文件实体操作repository
 * 4.包含文件配置对象 fileProperties
 */
@Component
@Slf4j
@Data
public class FileUtil {
    /**
     * 格式化小数
     */
    private static final DecimalFormat DF = new DecimalFormat("0.00");

    private final FastFileStorageClient storageClient;

    private FileProperties fileProperties;

    private FileUploadRepository fileUploadRepository;

    public FileUtil(FastFileStorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @Autowired
    public FileUtil(FastFileStorageClient storageClient, FileProperties fileProperties, FileUploadRepository fileUploadRepository) {
        this.storageClient = storageClient;
        this.fileProperties = fileProperties;
        this.fileUploadRepository = fileUploadRepository;
    }

    /**
     * 文件上传统一入口
     * @param inputStream  文件输入流
     * @param size  文件大小
     * @param fileName  文件名称
     * @return
     */
    public String upload(InputStream inputStream, Long size, String fileName) throws IOException {
        //判断文件上传方式
        if(StringUtils.isNotBlank(fileProperties.getFileUploadType())) {
            switch (fileProperties.getFileUploadType()) {
                case "fastdfs":
                    return fdfsUpload(inputStream, size, fileName);
                case "system":
                    return systemUpload(inputStream, size, fileName);
                default:
                    throw new FileUploadException("文件上传方式配置错误");
            }
        }else {
            throw new FileUploadException("文件上传方式配置为空！");
        }
    }

    /**
     * fastdfs文件上传方法
     * @param inputStream  文件输入流
     * @param size 文件大小
     * @param fileName  文件名
     * @return fullPath 返回文件访问地址
     */
    public String fdfsUpload(InputStream inputStream, Long size, String fileName) {
        log.info("fdfs上传文件开始，文件名：" + fileName + "  文件大小：" + getSize(size));
        StorePath storePath = storageClient.uploadFile(inputStream, size, StringUtils.substringAfterLast(fileName, "."), null);
        String fullPath = storePath.getFullPath();
        String returnPath = fileProperties.getDomain() + "/" + fullPath;
        log.info("fdfs文件上传成功！文件名：" + fileName + "   返回地址：" + returnPath);
        return returnPath;
    }

    /**
     * 将文件名解析成文件的上传路径
     */
    public String systemUpload(InputStream fileInputStream, Long size, String fileName) throws IOException {
        log.info("system上传文件开始，文件名：" + fileName + "  文件大小：" +  getSize(size));
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmssS");
        String suffix = StringUtils.substringAfterLast(fileName, ".");
        String nowStr = format.format(date);
        FileOutputStream outputStream = null;
        try {
            fileName = nowStr + "." + suffix;
            String path = getSavePath(fileName);
            // getCanonicalFile 可解析正确各种路径
            File dest = new File(path).getCanonicalFile();
            // 检测是否存在目录
            if (!dest.getParentFile().exists()) {
                if (!dest.getParentFile().mkdirs()) {
                    throw new FileUploadException("system文件上传异常创建文件保存目录失败！");
                }
            }
            // 文件写入
            outputStream = new FileOutputStream(path);
            byte[] buffer = new byte[8192];
            while (true) {
                int readLength = fileInputStream.read(buffer);
                if(readLength == -1) {
                    break;
                }
                outputStream.write(buffer, 0, readLength);
            }
            String returnPath = getReturnPath(path);
            log.info("fdfs文件上传成功！文件名：" + fileName + "  返回地址：" + returnPath);
            return fileProperties.getDomain() + returnPath;
        } finally {
            if(fileInputStream != null) {
                fileInputStream.close();
            }
            if(outputStream != null) {
                outputStream.close();
            }
        }
    }

    public static String getFileType(String type) {
        String documents = "txt doc pdf ppt pps xlsx xls docx";
        String music = "mp3 wav wma mpa ram ra aac aif m4a";
        String video = "avi mpg mpe mpeg asf wmv mov qt rm mp4 flv m4v webm ogv ogg";
        String image = "bmp dib pcp dif wmf gif jpg tif eps psd cdr iff tga pcd mpt png jpeg";
        if (image.contains(type)) {
            return FileTypeConstant.IMAGE;
        } else if (documents.contains(type)) {
            return FileTypeConstant.DOC;
        } else if (music.contains(type)) {
            return FileTypeConstant.MUSIC;
        } else if (video.contains(type)) {
            return FileTypeConstant.VIDEO;
        } else {
            return FileTypeConstant.OTHER;
        }
    }

    /**
     * 文件大小转换
     */
    public static String getSize(long size) {
        String resultSize;
        if (size / FileSizeConstant.GB >= 1) {
            // 如果当前Byte的值大于等于1GB
            resultSize = DF.format(size / (float) FileSizeConstant.GB) + "GB";
        } else if (size / FileSizeConstant.MB >= 1) {
            // 如果当前Byte的值大于等于1MB
            resultSize = DF.format(size / (float) FileSizeConstant.MB) + "MB";
        } else if (size / FileSizeConstant.KB >= 1) {
            // 如果当前Byte的值大于等于1KB
            resultSize = DF.format(size / (float) FileSizeConstant.KB) + "KB";
        } else {
            resultSize = size + "B";
        }
        return resultSize;
    }

    /**
     * 获取文件保存目录
     * @param fileName  文件名
     * @return
     */
    public String getSavePath(String fileName) {
        return fileProperties.getPath() + getFileType(StringUtils.substringAfterLast(fileName, ".")) + "\\" + fileName;
    }


    /**
     * 检查文件大小
     * @param size 上传文件大小
     */
    public void checkSize(String fileName, long size) {
        int maxSize = fileProperties.getFileMaxSize() * FileSizeConstant.MB;
        if (size > maxSize) {
            throw new FileUploadException("文件大小超出限制： " + fileProperties.getFileMaxSize() + "M   文件名：" + fileName);
        }
    }

    /**
     * 通过文件保存路径转换为返回路径
     * @param fileSavePath
     * @return String
     */
    public String getReturnPath(String fileSavePath) {
        String separator = "/|\\\\";
        String path = fileProperties.getPath();
        String[] arr = path.split(separator);
        String directoryName = "/" + arr[arr.length - 1] + "/";
        return directoryName + fileSavePath.replace(path, "").replace("\\","/");
    }
}
