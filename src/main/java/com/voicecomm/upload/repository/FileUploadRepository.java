package com.voicecomm.upload.repository;

import com.voicecomm.upload.entity.SysFileUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @ClassName FileUploadRepository
 * @Author yulin.li
 * @Date 2021/12/24 16:34
 * @Description FileUploadRepository
 */
public interface FileUploadRepository extends JpaRepository<SysFileUpload, String> {
    List<SysFileUpload> findByFileIdIn(List<String> ids);
}
