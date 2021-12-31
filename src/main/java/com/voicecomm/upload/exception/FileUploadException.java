package com.voicecomm.upload.exception;

import lombok.Data;

/**
 * @ClassName FileUploadException
 * @Author yulin.li
 * @Date 2021/12/23 14:43
 * @Description FileUploadException  自定义异常对象
 */
@Data
public class FileUploadException extends RuntimeException{
    /**
     * 异常信息
     */
    private String errorMsg;

    public FileUploadException(String errorMsg) {
        super(errorMsg);
    }
}
