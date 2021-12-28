package com.voicecomm.upload.handler;

import com.voicecomm.upload.util.FileUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;

/**
 * @ClassName ChannelInitializerHandler
 * @Author yulin.li
 * @Date 2021/12/21 15:02
 * @Description ChannelInitializerHandler
 */
@Slf4j
public class ChannelInitializerHandler extends ChannelInitializer<SocketChannel> {
    private FileUtil fdfsUtil;

    public ChannelInitializerHandler(FileUtil fastDFSUtil) {
        this.fdfsUtil = fastDFSUtil;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new HttpServerCodec())  //http请求编解码器
                .addLast(new FileUploadHandler(fdfsUtil)); //文件上传处理的handler
    }
}
