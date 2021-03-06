package com.voicecomm.upload.config;

import com.voicecomm.upload.server.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @ClassName NettyConfig
 * @Author yulin.li
 * @Date 2021/12/22 13:58
 * @Description NettyConfig netty配置类
 */
@Configuration
public class NettyConfig implements ApplicationListener<ContextRefreshedEvent> {
    private final ApplicationContext context;

    public NettyConfig(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        //监听spring容器 当容器初始化完成时，启动netty服务器
        context.getBean(NettyServer.class).run();
    }
}
