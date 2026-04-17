package aliang.flowcopilot.service.impl;

import aliang.flowcopilot.service.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文档本地存储实现。
 * <p>
 * 按 `知识库 ID / 文档 ID / 文件名` 的层级结构把上传文件落到本地目录中，
 * 同时负责路径解析和物理删除。
 */
@Service
@Slf4j
public class DocumentStorageServiceImpl implements DocumentStorageService {

    @Value("${document.storage.base-path:./data/documents}")
    private String baseStoragePath;

    @Override
    /**
     * 将上传文件保存到本地磁盘。
     *
     * @param kbId 知识库 ID
     * @param documentId 文档 ID
     * @param file 上传文件
     * @return 相对存储路径
     * @throws IOException 文件写入失败时抛出
     */
    public String saveFile(String kbId, String documentId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件为空");
        }

        // 构建文件存储路径: basePath/kbId/documentId/filename
        Path kbDir = Paths.get(baseStoragePath, kbId);
        Path documentDir = kbDir.resolve(documentId);
        
        // 确保目录存在
        Files.createDirectories(documentDir);
        
        // 生成唯一文件名（使用 UUID + 原始文件名）
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // 保存文件
        Path targetPath = documentDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // 返回相对路径（相对于 baseStoragePath）
        String relativePath = Paths.get(kbId, documentId, uniqueFilename).toString().replace("\\", "/");
        log.info("文件保存成功: kbId={}, documentId={}, filename={}, path={}", 
                kbId, documentId, originalFilename, relativePath);
        
        return relativePath;
    }

    @Override
    /**
     * 删除指定相对路径对应的文件。
     * <p>
     * 删除文件后会尝试清理空目录，但不会因为目录清理失败而中断流程。
     *
     * @param filePath 相对文件路径
     * @throws IOException 文件删除失败时抛出
     */
    public void deleteFile(String filePath) throws IOException {
        Path fullPath = getFilePath(filePath);
        if (Files.exists(fullPath)) {
            Files.delete(fullPath);
            log.info("文件删除成功: {}", filePath);
            
            // 尝试删除空的父目录
            Path parentDir = fullPath.getParent();
            if (parentDir != null && Files.exists(parentDir)) {
                try {
                    Files.delete(parentDir);
                    log.info("目录删除成功: {}", parentDir);
                } catch (IOException e) {
                    // 目录不为空或其他原因无法删除，忽略
                    log.debug("目录删除失败（可能不为空）: {}", parentDir);
                }
            }
        } else {
            log.warn("文件不存在，跳过删除: {}", filePath);
        }
    }

    @Override
    /**
     * 解析文件完整路径。
     *
     * @param filePath 相对文件路径
     * @return 完整路径对象
     */
    public Path getFilePath(String filePath) {
        return Paths.get(baseStoragePath, filePath);
    }

    @Override
    /**
     * 检查文件是否真实存在。
     *
     * @param filePath 相对文件路径
     * @return 是否存在且为普通文件
     */
    public boolean fileExists(String filePath) {
        Path fullPath = getFilePath(filePath);
        return Files.exists(fullPath) && Files.isRegularFile(fullPath);
    }
}
