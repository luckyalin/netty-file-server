package com.voicecomm.upload.server;

//import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
//import com.alibaba.cloud.nacos.NacosServiceManager;
//import com.alibaba.nacos.api.naming.NamingFactory;
//import com.alibaba.nacos.api.naming.NamingService;
import com.voicecomm.upload.config.NettyProperties;
import com.voicecomm.upload.handler.ChannelInitializerHandler;
import com.voicecomm.upload.util.FileUtil;
import com.voicecomm.upload.util.IpUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @ClassName NettyServer
 * @Author yulin.li
 * @Date 2021/12/21 14:49
 * @Description NettyServer
 */
@Data
@Slf4j
@Component
public class NettyServer {
    @Autowired
    private NettyProperties nettyProperties;

    @Autowired
    private FileUtil fdfsUtil;

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup workGroup;

    private ServerBootstrap serverBootstrap;

//    private NacosServiceManager nacosServiceManager;
//
//    private final NacosDiscoveryProperties properties;

    public void run () {
        try {
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
            serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(bossGroup, workGroup)
                    //使用NioSocketChannel 作为服务器的通道实现
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 给workerGroup 的 EventLoop 对应的管道设置处理器
                    .childHandler(new ChannelInitializerHandler(fdfsUtil));
            ChannelFuture channelFuture = serverBootstrap.bind(nettyProperties.getPort()).sync();

            //监听netty服务启动成功
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()) {
                        log.info("netty服务启动成功 端口：" + nettyProperties.getPort());
                        //启动成功后将服务手动注册都nacos
//                        NamingService service = NamingFactory.createNamingService(properties.getNacosProperties());
//                        service.registerInstance(nettyProperties.getServerName(),properties.getGroup(),
//                                IpUtils.getLocalHostIp(), nettyProperties.getPort());
                    }
                }
            });

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("netty服务启动异常：" + e);
        }finally{
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
