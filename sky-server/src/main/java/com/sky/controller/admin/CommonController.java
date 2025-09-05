package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.exception.IllegalPathException;
import com.sky.properties.UploadDirProperties;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController  // 所有方法默认都会把返回值直接序列化成 JSON 写回客户端
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private UploadDirProperties uploadDirProperties;

    /**
     * 文件上传
     * @RequestParam("file")表示把 HTTP 表单中 name="file" 的那一块数据取出来，封装成 MultipartFile 对象并注入到参数里
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(@RequestParam("file") MultipartFile file){
        log.info("文件上传:{}", file);

        if(file.isEmpty()){
            return Result.error(MessageConstant.FILE_NOT_EMPTY);
        }

        File dir = new File(uploadDirProperties.getDir());
        if(!dir.exists() || !dir.isDirectory()){
            boolean created = dir.mkdirs(); // File 类方法,一次性创建多级目录(包括所有不存在的父目录)，如果成功返回 true，失败返回 false
            if(created){
                log.info("创建文件夹成功:{}", uploadDirProperties.getDir());
            }else{
                log.warn("创建文件夹失败或已经存在:{}", uploadDirProperties.getDir());
            }
        }

        // 获取原始文件名:获取上传文件在浏览器端的原始文件名(不含路径，仅文件名 + 后缀)
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null || originalFilename.isEmpty()){
            return Result.error(MessageConstant.FILE_NAME_INVALID);
        }

        // 获取后缀,如"a.jpg"就获取"jpg"
        // 获取最后一个"."的index
        int index = originalFilename.lastIndexOf(".");
        // 获取后缀,截取原始文件名后缀
        String extention = originalFilename.substring(index);
        // 判断上传上来的文件是否符合是"图片"需求
        if(!extention.equalsIgnoreCase(".png") && !extention.equalsIgnoreCase(".jpg")
            && !extention.equalsIgnoreCase(".jpeg")){
            return Result.error(MessageConstant.FILE_FORMAT_NOT_SUPPORTED);
        }
        // UUID拼接,构造唯一的文件名(不能重复) - uuid 通用唯一识别码 4f7e3efc-6002-493d-a6b2-52b12675e099
        String newFileName = UUID.randomUUID().toString() + extention;
        log.info("新的文件名:{}", newFileName);

        // 计算拼接后的归一化路径
        Path targetLocation = Paths.get(uploadDirProperties.getDir()).resolve(newFileName).normalize();
        // 校验路径是否越界
        if (!targetLocation.startsWith(Paths.get(uploadDirProperties.getDir()))) {
            throw new IllegalPathException(MessageConstant.ILLEGAL_PATH);
        }
        try {
            // 把内存里的文件流(二进制输入流)，复制成磁盘上的真实文件，存在就覆盖
            // Files.copy(...)	JDK NIO 工具类，一次性把输入流拷贝到磁盘，自动关闭流
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功:{}", newFileName);
        } catch (IOException e) {
            // 把失败文件名和异常堆栈一次性写进日志
            log.error("文件上传失败:{}", newFileName, e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }

        // 你可以根据实际情况调整返回的文件访问链接
        String fileUrl = "http://localhost:8081/upload/" + newFileName;

        return Result.success(fileUrl);
    }
}
