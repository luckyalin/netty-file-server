package com.voicecomm.upload.entity.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @ClassName UploadInfoDTO
 * @Author yulin.li
 * @Date 2021/12/27 15:24
 * @Description UploadInfoDTO  文件上传响应实体
 */
@Data
public class UploadInfoDTO implements Serializable {
    /**
     * 上传开始时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss:SSS")
    private Date beginTime;

    /**
     * 上传结束时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss:SSS")
    private Date endTime;

    /**
     * 总耗时
     */
    private Long totalTime;

    /**
     * 文件上传信息集合
     */
    List<FileInfoDTO> fileInfos;
}
