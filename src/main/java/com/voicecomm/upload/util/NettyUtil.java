package com.voicecomm.upload.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.voicecomm.upload.entity.dto.FileInfoDTO;
import com.voicecomm.upload.entity.dto.NettyRequestDTO;
import com.voicecomm.upload.entity.SysFileUpload;
import com.voicecomm.upload.entity.dto.UploadInfoDTO;
import com.voicecomm.upload.exception.FileUploadException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;
import static io.netty.buffer.Unpooled.copiedBuffer;

/**
 * @ClassName NettyFileUtils
 * @Author yulin.li
 * @Date 2021/12/24 11:12
 * @Description NettyFileUtils
 */
@Component
@Slf4j
public class NettyUtil {
    private static final String FILE_PARAM_NAME = "file";
    /**
     * netty上传文件方法
     * @param fileUtil  文件上传工具类
     * @param decoder  文件解码器
     * @param beginTime 上传开始时间
     * @return
     * @throws IOException
     */
    public static String upload(FileUtil fileUtil, HttpPostRequestDecoder decoder, Date beginTime) throws IOException{
        Date endTime = null;
        //解析所有上传文件，封装成一个对象
        NettyRequestDTO multipartBody = getMultipartBody(decoder, fileUtil);
        if (multipartBody != null && CollectionUtil.isEmpty(multipartBody.getFileUploads())) {
            throw new FileUploadException("上传文件为空");
        }
        //先根据上传的文件集合 封装数据库对象 其中包含上传的文件对象fileUpload
        List<SysFileUpload> fileUploads = fullFileUpload(multipartBody.getFileUploads(), fileUtil.getFileProperties().getFileUploadType(), beginTime);
        //将所有的文件对象入库
        fileUtil.getFileUploadRepository().saveAll(fileUploads);
        //初始化响应对象
        UploadInfoDTO uploadInfoDTO = new UploadInfoDTO();
        uploadInfoDTO.setBeginTime(fileUploads.get(0).getBeginTime());
        //初始化循环中的变量 循环始终使用一个变量接收
        SysFileUpload sysFileUpload = null;
        FileUpload fileUpload = null;
        InputStream in = null;
        //遍历所有文件对象
        for (int i = 0; i < fileUploads.size(); i++) {
            sysFileUpload = fileUploads.get(i);
            //获取文件对象
            fileUpload = sysFileUpload.getFileUpload();
            try {
                //获取文件流
                in = new FileInputStream(fileUpload.getFile());
                //调用上传方法
                String downUrl = fileUtil.upload(in, fileUpload.length(), fileUpload.getFilename());
                sysFileUpload.setDownloadUrl(downUrl);
                sysFileUpload.setStatus(1);
            } catch (Exception e) {
                sysFileUpload.setStatus(2);
                log.error("文件上传失败，文件名：" + sysFileUpload.getFileName() + "    错误原因：" + e.getMessage());
            }finally {
                in.close();
            }
            //封装当前文件结束时间  总耗时
            endTime = new Date();
            Long totalTime = endTime.getTime() - fileUploads.get(0).getBeginTime().getTime();
            sysFileUpload.setEndTime(endTime);
            sysFileUpload.setTotalTime(totalTime);
            decoder.removeHttpDataFromClean(fileUpload);
            if(i == fileUploads.size() - 1) {
                uploadInfoDTO.setEndTime(endTime);
                uploadInfoDTO.setTotalTime(totalTime);
            }
        }
        List<FileInfoDTO> fileInfos = BeanUtil.copyList(fileUploads, FileInfoDTO.class);
        uploadInfoDTO.setFileInfos(fileInfos);
        fileUtil.getFileUploadRepository().saveAll(fileUploads);
        return JSON.toJSONString(uploadInfoDTO);
    }

    /**
     * 根据文件id集合查询文件信息
     * @param fileUtil
     * @param request
     */
    public static String getFileInfoByIds(FileUtil fileUtil, HttpRequest request) throws IOException {
        //获取请求参数
        Map<String, Object> params = readGetParams(request);
        String ids = (String) params.get("ids");
        log.info("获取文件信息请求参数：" + ids);
        List<String> idList = JSON.parseArray(ids, String.class);
        //根据id集合查询文件信息
        List<SysFileUpload> byFileIdIn = fileUtil.getFileUploadRepository().findByFileIdIn(idList);
        List<FileInfoDTO> fileInfoDtoS = BeanUtil.copyList(byFileIdIn, FileInfoDTO.class, true);
        return JSON.toJSONString(fileInfoDtoS);
    }

    /**
     * 读取GET方法的参数
     * @param request
     * @return
     */
    public static Map<String, Object> readGetParams(HttpRequest request) {
        if (request.method() == HttpMethod.GET) {
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            Map<String, Object> paramMap = new HashMap<>(decoder.parameters().size());
            decoder.parameters().entrySet().forEach(entry -> {
                paramMap.put(entry.getKey(), entry.getValue().get(0));
            });
            return paramMap;
        }
        return null;
    }

    /**
     * 解析上传文件对象
     * @param decoder  解码器对象
     * @return
     */
    public static NettyRequestDTO getMultipartBody(HttpPostRequestDecoder decoder, FileUtil fileUtil) throws IOException {
        NettyRequestDTO multipartRequest = new NettyRequestDTO();
        //存放文件对象
        List<FileUpload> fileUploads = new ArrayList<>();
        //通过迭代器获取HTTP的内容
        List<InterfaceHttpData> interfaceHttpDataList = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : interfaceHttpDataList) {
            //如果数据类型为文件类型，则保存到fileUploads对象中 如果参数名不对则抛出异常
            if (data != null && InterfaceHttpData.HttpDataType.FileUpload.equals(data.getHttpDataType()) && data.getName().equals(FILE_PARAM_NAME)) {
                FileUpload fileUpload = (FileUpload) data;
                fileUtil.checkSize(fileUpload.getFilename(), fileUpload.length());
                fileUploads.add(fileUpload);
            }else {
                throw new FileUploadException("文件上传参数错误");
            }
        }
        //存放文件信息
        multipartRequest.setFileUploads(fileUploads);
        //存放参数信息
        return multipartRequest;
    }

    /**
     * 解析上传文件对象
     * @param decoder  解码对象
     * @return
     */
    public static NettyRequestDTO getRequestBody(HttpPostRequestDecoder decoder) throws IOException {
        NettyRequestDTO nettyRequest = new NettyRequestDTO();
        //存放参数对象
        JSONObject body = new JSONObject();
        //通过迭代器获取HTTP的内容
        List<InterfaceHttpData> interfaceHttpDataList = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : interfaceHttpDataList) {
            //如果数据类型为参数类型，则保存到body对象中
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                Attribute attribute = (Attribute) data;
                body.put(attribute.getName(), attribute.getValue());
            }
        }
        //存放参数信息
        nettyRequest.setParams(body);
        return nettyRequest;
    }

    /**
     * 返回内容给前端
     * @param channel  当前请求channel
     * @param request  当前请求request对象
     * @param responseContent  返回的内容
     * @param forceClose  返回后是否关闭channel
     */
    public static void writeResponse(Channel channel,HttpRequest request,StringBuilder responseContent, HttpResponseStatus status, boolean forceClose) {
        // 将响应内容转换为buffer
        ByteBuf buf = copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
        responseContent.setLength(0);

        // 根据forceClose判断是否关闭channel
        boolean keepAlive = HttpUtil.isKeepAlive(request) && !forceClose;

        // 构建http响应对象
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, buf);
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
        // 写出响应
        ChannelFuture future = channel.writeAndFlush(response);
        // 添加关闭channel监听
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }


    /**
     * 文件下载方法
     * @param ctx  当前请求上下文对象
     * @param path 请求文件地址
     */
    public static void responseExportFile(ChannelHandlerContext ctx, String path) throws FileNotFoundException, IOException {
        File file = new File(path);
        //随机读取文件
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        long fileLength = raf.length();
        //定义response对象
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        //设置请求头部
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream; charset=UTF-8");
        response.headers().add(HttpHeaderNames.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getName() + "\";");
        ctx.write(response);
        //设置事件通知对象
        ChannelFuture sendFileFuture = ctx
                .write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
        sendFileFuture.addListener(ChannelFutureListener.CLOSE);
        //刷新缓冲区数据，文件结束标志符
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    /**
     * 封装文件信息集合
     * @param fileUploads  文件上传fileUpload集合
     * @return
     */
    public static List<SysFileUpload> fullFileUpload (List<FileUpload> fileUploads, String uploadType, Date beginTime) {
        List<SysFileUpload> sysFileUploadlist = new ArrayList<>();
        for (FileUpload fileUpload : fileUploads) {
        	sysFileUploadlist.add(convertFromFileUpload(fileUpload, uploadType, beginTime));
        }
        return sysFileUploadlist;
    }

    /**
     * 通过fileUpload封装sysFileUpload
     * @param fileUpload
     * @return
     */
    public static SysFileUpload convertFromFileUpload(FileUpload fileUpload, String uploadType, Date beginTime) {
        SysFileUpload sysFileUpload = new SysFileUpload();
        sysFileUpload.setFileId(UUID.randomUUID().toString(true));
        sysFileUpload.setFileName(fileUpload.getFilename());
        sysFileUpload.setFileSize(Integer.parseInt(String.valueOf(fileUpload.length())));
        sysFileUpload.setExtension(StringUtils.substringAfterLast(fileUpload.getFilename(), "."));
        sysFileUpload.setStatus(0);
        sysFileUpload.setBeginTime(beginTime);
        sysFileUpload.setCreateTime(new Date());
        sysFileUpload.setUploadType(uploadType);
        sysFileUpload.setFileUpload(fileUpload);
//        sysFileUpload.setCreateBy(); //TODO
        return sysFileUpload;
    }

}
