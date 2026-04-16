package aliang.flowcopilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import aliang.flowcopilot.converter.DocumentConverter;
import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.DocumentMapper;
import aliang.flowcopilot.model.dto.DocumentDTO;
import aliang.flowcopilot.model.entity.Document;
import aliang.flowcopilot.model.request.CreateDocumentRequest;
import aliang.flowcopilot.model.request.UpdateDocumentRequest;
import aliang.flowcopilot.model.response.CreateDocumentResponse;
import aliang.flowcopilot.model.response.GetDocumentsResponse;
import aliang.flowcopilot.model.vo.DocumentVO;
import aliang.flowcopilot.mapper.ChunkBgeM3Mapper;
import aliang.flowcopilot.model.entity.ChunkBgeM3;
import aliang.flowcopilot.service.DocumentFacadeService;
import aliang.flowcopilot.service.DocumentStorageService;
import aliang.flowcopilot.service.MarkdownParserService;
import aliang.flowcopilot.service.RagService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档门面服务实现。
 * <p>
 * 负责文档记录管理、文件存储以及 Markdown 文档的解析、切块和向量化入库，
 * 是知识库 RAG 数据准备链路的核心实现。
 */
@Service
@AllArgsConstructor
@Slf4j
public class DocumentFacadeServiceImpl implements DocumentFacadeService {

    private final DocumentMapper documentMapper;
    private final DocumentConverter documentConverter;
    private final DocumentStorageService documentStorageService;
    private final MarkdownParserService markdownParserService;
    private final RagService ragService;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;

    @Override
    /**
     * 查询全部文档并转换为展示对象。
     *
     * @return 文档列表响应
     */
    public GetDocumentsResponse getDocuments() {
        List<Document> documents = documentMapper.selectAll();
        List<DocumentVO> result = new ArrayList<>();
        for (Document document : documents) {
            try {
                DocumentVO vo = documentConverter.toVO(document);
                result.add(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return GetDocumentsResponse.builder()
                .documents(result.toArray(new DocumentVO[0]))
                .build();
    }

    @Override
    /**
     * 查询指定知识库下的文档。
     *
     * @param kbId 知识库 ID
     * @return 文档列表响应
     */
    public GetDocumentsResponse getDocumentsByKbId(String kbId) {
        List<Document> documents = documentMapper.selectByKbId(kbId);
        List<DocumentVO> result = new ArrayList<>();
        for (Document document : documents) {
            try {
                DocumentVO vo = documentConverter.toVO(document);
                result.add(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return GetDocumentsResponse.builder()
                .documents(result.toArray(new DocumentVO[0]))
                .build();
    }

    @Override
    /**
     * 仅创建文档业务记录，不处理文件上传和向量化。
     *
     * @param request 文档创建请求
     * @return 新建文档 ID
     */
    public CreateDocumentResponse createDocument(CreateDocumentRequest request) {
        try {
            // 将 CreateDocumentRequest 转换为 DocumentDTO
            DocumentDTO documentDTO = documentConverter.toDTO(request);

            // 将 DocumentDTO 转换为 Document 实体
            Document document = documentConverter.toEntity(documentDTO);

            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            document.setCreatedAt(now);
            document.setUpdatedAt(now);

            // 插入数据库，ID 由数据库自动生成
            int result = documentMapper.insert(document);
            if (result <= 0) {
                throw new BizException("创建文档失败");
            }

            // 返回生成的 documentId
            return CreateDocumentResponse.builder()
                    .documentId(document.getId())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建文档时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    /**
     * 上传文档并处理后续业务。
     * <p>
     * 流程包括：
     * 1. 创建文档记录；
     * 2. 保存物理文件；
     * 3. 回写文件路径到元数据；
     * 4. 若为 Markdown，则继续解析、切块并写入向量表。
     *
     * @param kbId 知识库 ID
     * @param file 上传文件
     * @return 新建文档 ID
     */
    public CreateDocumentResponse uploadDocument(String kbId, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new BizException("上传的文件为空");
            }

            // 提取文件信息
            String originalFilename = file.getOriginalFilename();
            String filetype = getFileType(originalFilename);
            long fileSize = file.getSize();

            // 创建文档记录（先创建记录，获取 documentId）
            DocumentDTO documentDTO = DocumentDTO.builder()
                    .kbId(kbId)
                    .filename(originalFilename)
                    .filetype(filetype)
                    .size(fileSize)
                    .build();

            Document document = documentConverter.toEntity(documentDTO);
            LocalDateTime now = LocalDateTime.now();
            document.setCreatedAt(now);
            document.setUpdatedAt(now);

            // 插入数据库，获取生成的 documentId
            int result = documentMapper.insert(document);
            if (result <= 0) {
                throw new BizException("创建文档记录失败");
            }

            String documentId = document.getId();

            // 保存文件
            String filePath = documentStorageService.saveFile(kbId, documentId, file);

            // 更新文档记录，保存文件路径到 metadata
            DocumentDTO.MetaData metadata = new DocumentDTO.MetaData();
            metadata.setFilePath(filePath);
            documentDTO.setMetadata(metadata);
            documentDTO.setId(documentId);
            documentDTO.setCreatedAt(now);
            documentDTO.setUpdatedAt(now);

            Document updatedDocument = documentConverter.toEntity(documentDTO);
            updatedDocument.setId(documentId);
            updatedDocument.setCreatedAt(now);
            updatedDocument.setUpdatedAt(now);

            documentMapper.updateById(updatedDocument);

            log.info("文档上传成功: kbId={}, documentId={}, filename={}", kbId, documentId, originalFilename);

            // 如果是 Markdown 文件，进行解析并生成 chunks
            if ("md".equalsIgnoreCase(filetype) || "markdown".equalsIgnoreCase(filetype)) {
                processMarkdownDocument(kbId, documentId, filePath);
            } else {
                // TODO: 未来可以增加其他文件类型的处理逻辑
                log.warn("待新增处理的文件类型: {}", filetype);
            }

            return CreateDocumentResponse.builder()
                    .documentId(documentId)
                    .build();
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException("文件保存失败: " + e.getMessage());
        }
    }

    @Override
    /**
     * 删除文档记录及对应文件。
     *
     * @param documentId 文档 ID
     */
    public void deleteDocument(String documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BizException("文档不存在: " + documentId);
        }

        // 删除文件
        try {
            DocumentDTO documentDTO = documentConverter.toDTO(document);
            if (documentDTO.getMetadata() != null && documentDTO.getMetadata().getFilePath() != null) {
                String filePath = documentDTO.getMetadata().getFilePath();
                documentStorageService.deleteFile(filePath);
            }
        } catch (Exception e) {
            log.warn("删除文件失败，继续删除文档记录: documentId={}, error={}", documentId, e.getMessage());
            // 即使文件删除失败，也继续删除数据库记录
        }

        // 删除数据库记录
        int result = documentMapper.deleteById(documentId);
        if (result <= 0) {
            throw new BizException("删除文档失败");
        }
    }

    /**
     * 处理 Markdown 文档并生成向量分块。
     * <p>
     * 每个章节标题会先被向量化，再与章节正文一起写入 `chunk_bge_m3`，
     * 供后续知识库检索使用。
     *
     * @param kbId 知识库 ID
     * @param documentId 文档 ID
     * @param filePath 已落盘文件的相对路径
     */
    private void processMarkdownDocument(String kbId, String documentId, String filePath) {
        try {
            log.info("开始处理 Markdown 文档: kbId={}, documentId={}, filePath={}", kbId, documentId, filePath);

            // 从保存的文件路径读取文件
            Path path = documentStorageService.getFilePath(filePath);
            try (InputStream inputStream = Files.newInputStream(path)) {
                // 解析 Markdown 文件
                List<MarkdownParserService.MarkdownSection> sections = markdownParserService.parseMarkdown(inputStream);

                System.out.println(sections);

                if (sections.isEmpty()) {
                    log.warn("Markdown 文档解析后没有找到任何章节: documentId={}", documentId);
                    return;
                }

                LocalDateTime now = LocalDateTime.now();
                int chunkCount = 0;

                // 为每个章节生成 chunk
                for (MarkdownParserService.MarkdownSection section : sections) {
                    String title = section.getTitle();
                    String content = section.getContent();

                    if (title == null || title.trim().isEmpty()) {
                        continue;
                    }

                    // 对标题进行 embedding
                    float[] embedding = ragService.embed(title);

                    // 创建 ChunkBgeM3 实体
                    ChunkBgeM3 chunk = ChunkBgeM3.builder()
                            .kbId(kbId)
                            .docId(documentId)
                            .content(content != null ? content : "")
                            .metadata(null) // 可以存储标题信息到 metadata
                            .embedding(embedding)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    // 插入数据库
                    int result = chunkBgeM3Mapper.insert(chunk);

                    if (result > 0) {
                        chunkCount++;
                        log.debug("创建 chunk 成功: title={}, chunkId={}", title, chunk.getId());
                    } else {
                        log.warn("创建 chunk 失败: title={}", title);
                    }
                }
                log.info("Markdown 文档处理完成: documentId={}, 共生成 {} 个 chunks", documentId, chunkCount);
            }
        } catch (Exception e) {
            log.error("处理 Markdown 文档失败: documentId={}", documentId, e);
            // 不抛出异常，避免影响文档上传流程
        }
    }

    /**
     * 从文件名中提取扩展名作为文件类型。
     *
     * @param filename 原始文件名
     * @return 小写文件扩展名；无法识别时返回 {@code unknown}
     */
    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    @Override
    /**
     * 更新文档元数据。
     *
     * @param documentId 文档 ID
     * @param request 更新请求
     */
    public void updateDocument(String documentId, UpdateDocumentRequest request) {
        try {
            // 查询现有的文档
            Document existingDocument = documentMapper.selectById(documentId);
            if (existingDocument == null) {
                throw new BizException("文档不存在: " + documentId);
            }

            // 将现有 Document 转换为 DocumentDTO
            DocumentDTO documentDTO = documentConverter.toDTO(existingDocument);

            // 使用 UpdateDocumentRequest 更新 DocumentDTO
            documentConverter.updateDTOFromRequest(documentDTO, request);

            // 将更新后的 DocumentDTO 转换回 Document 实体
            Document updatedDocument = documentConverter.toEntity(documentDTO);

            // 保留原有的 ID、kbId 和创建时间
            updatedDocument.setId(existingDocument.getId());
            updatedDocument.setKbId(existingDocument.getKbId());
            updatedDocument.setCreatedAt(existingDocument.getCreatedAt());
            updatedDocument.setUpdatedAt(LocalDateTime.now());

            // 更新数据库
            int result = documentMapper.updateById(updatedDocument);
            if (result <= 0) {
                throw new BizException("更新文档失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新文档时发生序列化错误: " + e.getMessage());
        }
    }
}
