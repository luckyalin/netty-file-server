package com.voicecomm.upload.entity.dto;

import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.multipart.FileUpload;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * @ClassName NettyRequestDTO
 * @Author yulin.li
 * @Date 2021/12/24 16:42
 * @Description NettyRequestDTO  接收请求实体
 */
@Data
public class NettyRequestDTO implements Serializable {
    /**
     * 接收到的上传文件集合
     */
    private List<FileUpload> fileUploads;

    /**
     * 接收到的参数对象
     */
    private JSONObject params;
}