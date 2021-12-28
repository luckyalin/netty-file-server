package com.voicecomm.upload.config;

import com.voicecomm.upload.constants.ElAdminConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName FileProperties
 * @Author yulin.li
 * @Date 2021/12/23 13:51
 * @Description FileProperties   文件上传配置对象
 */
@Component
@ConfigurationProperties(prefix = "file")
@Data
public class FileProperties {
    private String linuxPath;  //linux系统文件保存路径

    private String windowsPath;  //windows系统文件保存路径

    private Integer fileMaxSize;  //文件上传大小限制

    private String fileUploadType; //文件上传方式

    //netty服务域名地址
    private String domain;

    public String getPath() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith(ElAdminConstant.WIN)) {
            return windowsPath;
        }
        return linuxPath;
    }

}
