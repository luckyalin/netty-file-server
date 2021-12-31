package com.voicecomm.upload.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voicecomm.upload.util.FileUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName FileAsyncDTO
 * @Author yulin.li
 * @Date 2021/12/24 16:42
 * @Description FileAsyncDTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileInfoDTO implements Serializable {
    /**
     * 文件id
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小
     */
    private String fileSize;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 下载地址
     */
    private String downloadUrl;

    /**
     * 上传状态
     */
    private Integer status;

    /**
     * 上传开始时间
     */
    private String beginTime;

    /**
     * 上传结束时间
     */
    private String endTime;

    /**
     * 上传总耗时
     */
    private Long totalTime;

    /**
     * 上传人
     */
    private String createBy;

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = FileUtil.getSize(fileSize);
    }
}
