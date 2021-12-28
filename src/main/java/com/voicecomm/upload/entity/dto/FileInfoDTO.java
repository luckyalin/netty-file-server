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
    private String fileId;  //文件id

    private String fileName; //文件名称

    private String fileSize; //文件大小

    private String extension; //文件扩展名

    private String downloadUrl;  //下载地址

    private Integer status;  //上传状态

    private String beginTime; //上传开始时间

    private String endTime;  //上传结束时间

    private Long totalTime;  //上传总耗时

    private String createBy; //上传人

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = FileUtil.getSize(fileSize);
    }
}
