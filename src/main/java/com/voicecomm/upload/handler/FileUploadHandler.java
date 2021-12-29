package com.voicecomm.upload.handler;

import com.voicecomm.upload.exception.FileUploadException;
import com.voicecomm.upload.util.FileUtil;
import com.voicecomm.upload.util.NettyUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * @ClassName NettyProperties
 * @Author yulin.li
 * @Date 2021/12/21 16:38
 * @Description 文件上传处理器
 */

@Slf4j
@Component
public class FileUploadHandler extends SimpleChannelInboundHandler<HttpObject> {
    private FileUtil fileUtil;

    //当前请求的request对象
    private HttpRequest request;

    //当前请求的解码器对象
    private HttpPostRequestDecoder decoder = null;

    //当前请求开始时间
    private Date beginTime = null;

    private HttpDataFactory factory = new DefaultHttpDataFactory(true);

    public FileUploadHandler(FileUtil fastDFSUtil) {
        this.fileUtil = fastDFSUtil;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if(beginTime == null) {
            this.beginTime = new Date();
        }
        StringBuilder responseContent = new StringBuilder();
        try {
            if (msg instanceof HttpRequest) {
                request = (HttpRequest)msg;
                //windows系统下的文件下载接口
                if(request.uri().startsWith("/voicecomm") && request.method().equals(HttpMethod.GET)) {
                    String voicecomm = fileUtil.getFileProperties().getPath().replace("\\", "/");
                    voicecomm = StringUtils.substringBefore(voicecomm, "/");
                    NettyUtil.responseExportFile(ctx, voicecomm + request.uri());

                //linux window系统文件上传接口  可同时上传多个  参数名file
                }else if(request.uri().startsWith("/upload") && request.method().equals(HttpMethod.POST)) {
                    decoder = new HttpPostRequestDecoder(factory, request);
                    decoder.setDiscardThreshold(0);

                //根据文件id集合查询文件信息
                }else if(request.uri().startsWith("/getFileInfoByIds") && request.method().equals(HttpMethod.GET)) {
                    String fileInfoStr = NettyUtil.getFileInfoByIds(fileUtil, request);
                    responseContent.append(fileInfoStr);
                    NettyUtil.writeResponse(ctx.channel(),request, responseContent, HttpResponseStatus.OK,true);
                    this.reset();

                //其他请求路径返回错误
                }else {
                    throw new FileUploadException("请求地址有误");
                }

            } else if(msg instanceof HttpContent && decoder != null) {
                HttpContent chunk = (HttpContent) msg;
                decoder.offer(chunk);
                if (chunk instanceof LastHttpContent) {
                    //如果文件接收完成则开始走上传业务
                    String uploadInfoStr = NettyUtil.upload(fileUtil, decoder, beginTime);
                    responseContent.append(uploadInfoStr);
                    NettyUtil.writeResponse(ctx.channel(),request, responseContent, HttpResponseStatus.OK,true);
                    this.reset();
                }
            }

        } catch (FileUploadException e) {
            log.error("业务异常：" + e.getMessage() + "  接口地址：" + (this.request != null ? request.uri() : ""));
            responseContent.append(e.getMessage());
            NettyUtil.writeResponse(ctx.channel(),request, responseContent,HttpResponseStatus.INTERNAL_SERVER_ERROR, true);
            //发生异常销毁decoder 防止内存溢出
            this.reset();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("请求异常：" + e.getMessage() + "  接口地址：" + (this.request != null ? request.uri() : ""));
            responseContent.append("系统异常");
            NettyUtil.writeResponse(ctx.channel(),request, responseContent,HttpResponseStatus.INTERNAL_SERVER_ERROR, true);
            //发生异常销毁decoder 防止内存溢出
            this.reset();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("netty服务异常：" + cause.getMessage());
        this.reset();
        ctx.channel().close();
    }

    public void reset() {
        this.beginTime = null;
        if(decoder != null) {
            this.decoder.destroy();
        }
        this.decoder = null;
    }

}
