package aliang.flowcopilot.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 文档物理存储服务接口。
 * <p>
 * 负责上传文件的落盘、路径解析、存在性判断以及物理文件删除，
 * 是文档业务记录与本地文件系统之间的桥梁。
 */
public interface DocumentStorageService {
    /**
     * 保存上传文件到本地存储目录。
     *
     * @param kbId 知识库 ID，用于构建目录层级
     * @param documentId 文档 ID，用于构建目录层级
     * @param file 上传的文件
     * @return 相对存储路径
     * @throws IOException 文件保存失败时抛出
     */
    String saveFile(String kbId, String documentId, MultipartFile file) throws IOException;

    /**
     * 删除指定相对路径对应的物理文件。
     *
     * @param filePath 相对文件路径
     * @throws IOException 文件删除失败时抛出
     */
    void deleteFile(String filePath) throws IOException;

    /**
     * 将相对路径解析为完整文件系统路径。
     *
     * @param filePath 相对文件路径
     * @return 完整绝对路径或可解析路径
     */
    Path getFilePath(String filePath);

    /**
     * 检查相对路径对应的文件是否存在。
     *
     * @param filePath 相对文件路径
     * @return 文件是否存在且为普通文件
     */
    boolean fileExists(String filePath);
}
