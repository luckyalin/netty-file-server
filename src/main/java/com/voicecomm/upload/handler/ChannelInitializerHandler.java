package com.voicecomm.upload.handler;

import com.voicecomm.upload.config.NettyProperties;
import com.voicecomm.upload.util.FastDFSUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName ChannelInitializerHandler
 * @Author yulin.li
 * @Date 2021/12/21 15:02
 * @Description ChannelInitializerHandler
 */
@Slf4j
public class ChannelInitializerHandler extends ChannelInitializer<SocketChannel> {
    private FastDFSUtil fdfsUtil;

    public ChannelInitializerHandler(FastDFSUtil fastDFSUtil) {
        this.fdfsUtil = fastDFSUtil;
    }
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new HttpRequestDecoder())  //http请求解码处理器
                .addLast(new HttpResponseEncoder()) //http请求编码处理器
                .addLast(new HttpContentCompressor())  //压缩http请求处理器
                .addLast(new FileUploadHandler(fdfsUtil)); //文件上传处理的handler
    }
}
