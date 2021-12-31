package com.voicecomm.upload.entity;

import io.netty.handler.codec.http.multipart.FileUpload;
import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName FileUpload
 * @Author yulin.li
 * @Date 2021/12/24 15:00
 * @Description FileUpload  文件数据库对应实体
 */
@Data
@Entity
@Table(name = "sys_file_upload")
public class SysFileUpload implements Serializable {

    /**
     * 文件id
     */
    @Id
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Integer fileSize;

    /**
     * 文件下载地址
     */
    private String downloadUrl;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件后缀名
     */
    private String extension;

    /**
     * 上传状态 0上传中 1上传成功 2上传失败
     */
    private Integer status;

    /**
     * 开始时间
     */
    private Date beginTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 总耗时
     */
    private Long totalTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 上传类型 0 linux 1 windows
     */
    private String uploadType;

    /**
     * 当前对象对应的文件上传对象
     */
    @Transient
    private FileUpload fileUpload;
}
