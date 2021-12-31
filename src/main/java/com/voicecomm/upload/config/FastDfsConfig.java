package com.voicecomm.upload.config;

import com.github.tobato.fastdfs.FdfsClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "fdfs")
@Component
@Import(FdfsClientConfig.class) // 导入FastDFS-Client组件
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING) // 解决jmx重复注册bean的问题
/**
 * @ClassName FastDFSConfig
 * @Author yulin.li
 * @Date 2021/12/21 17:39
 * @Description FastDFSConfig  fastdfs配置类
 */
public class FastDfsConfig {
}
