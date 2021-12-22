/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.voicecomm.upload.handler;

import com.voicecomm.upload.util.FastDFSUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import static io.netty.buffer.Unpooled.copiedBuffer;

/**
 * @ClassName NettyProperties
 * @Author yulin.li
 * @Date 2021/12/21 16:38
 * @Description 文件上传处理器
 */

@Slf4j
@Component
public class FileUploadHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger logger = Logger.getLogger(FileUploadHandler.class.getName());

    private HttpRequest request;

    private HttpData partialContent;

    private final StringBuilder responseContent = new StringBuilder();

    private static final HttpDataFactory factory =
            new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed

    private HttpPostRequestDecoder decoder;

    private FastDFSUtil fdfsUtil;

    public FileUploadHandler(FastDFSUtil fastDFSUtil) {
        this.fdfsUtil = fastDFSUtil;
    }

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (decoder != null) {
            decoder.cleanFiles();
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        try {
            if (msg instanceof HttpRequest) {
                request = (HttpRequest) msg;
                if (request.uri().contains("upload") && request.method().equals(HttpMethod.POST)) {
                    this.decoder = new HttpPostRequestDecoder(factory, request);
                    this.decoder.setDiscardThreshold(0);
                } else {
                    //传递给下一个Handler
                    ctx.fireChannelRead(msg);
                }
            }
            if (msg instanceof HttpContent) {
                // 收到请求块
                HttpContent chunk = (HttpContent) msg;
                this.decoder.offer(chunk);
                // 处理请求块
                readHttpDataChunkByChunk();

                // 如果请求块处理完成则相应请求
                if (chunk instanceof LastHttpContent) {
                    writeResponse(ctx.channel());
                    reset();
                }
            }
        } catch (Exception e) {
            log.error("文件上传发生异常：" + e);
            responseContent.append("上传异常");
            writeResponse(ctx.channel(), true);
            return;
        }
    }

    private void reset() {
        request = null;
        // destroy the decoder to release all resources
        decoder.destroy();
        decoder = null;
    }

    /**
     * Example of reading request by chunk and getting values from chunk to chunk
     */
    private void readHttpDataChunkByChunk() throws EndOfDataDecoderException,IOException {
        while (decoder.hasNext()) {
            InterfaceHttpData data = decoder.next();
            if (data != null) {
                // check if current HttpData is a FileUpload and previously set as partial
                if (partialContent == data) {
                    logger.info(" 100% (FinalSize: " + partialContent.length() + ")");
                    partialContent = null;
                }
                // 请求块全部接收完成后进行保存
                writeHttpData(data);
            }
        }
    }

    private void writeHttpData(InterfaceHttpData data) throws IOException {
        if (data.getHttpDataType() == HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) data;
            // 如果文件已收取完成，则写入到fastdfs
            if (fileUpload.isCompleted()) {
                byte[] bytes = fileUpload.get();
                InputStream inputStream = new ByteArrayInputStream(bytes);
                String fileUrl = fdfsUtil.upload(inputStream, fileUpload.length(), fileUpload.getFilename());
                decoder.removeHttpDataFromClean(fileUpload); //remove
                responseContent.append(fileUrl);
            }
        }
    }

    private void writeResponse(Channel channel) {
        writeResponse(channel, false);
    }

    private void writeResponse(Channel channel, boolean forceClose) {
        // Convert the response content to a ChannelBuffer.
        ByteBuf buf = copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
        responseContent.setLength(0);

        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(request) && !forceClose;

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());

        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        Set<Cookie> cookies;
        String value = request.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        if (!cookies.isEmpty()) {
            // Reset the cookies if necessary.
            for (Cookie cookie : cookies) {
                response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
            }
        }
        // Write the response.
        ChannelFuture future = channel.writeAndFlush(response);
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("netty服务异常：" + cause.getMessage());
        ctx.channel().close();
    }
}
