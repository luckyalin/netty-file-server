package com.voicecomm.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName NettyProperties
 * @Author yulin.li
 * @Date 2021/12/21 16:38
 * @Description NettyProperties
 */
@ConfigurationProperties(prefix = "netty.server")
@Component
@Data
public class NettyProperties {
    // netty服务端口
    private Integer port;

    // netty服务注册名称
    private String serverName;

    //netty服务域名地址
    private String domain;
}
