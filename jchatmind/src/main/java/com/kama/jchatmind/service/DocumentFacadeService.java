package com.kama.jchatmind.service;

import com.kama.jchatmind.model.request.CreateDocumentRequest;
import com.kama.jchatmind.model.request.UpdateDocumentRequest;
import com.kama.jchatmind.model.response.CreateDocumentResponse;
import com.kama.jchatmind.model.response.GetDocumentsResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档门面服务。
 * <p>
 * 对外提供文档记录管理、文件上传、知识库归档以及 Markdown 文档向量化入库能力。
 */
public interface DocumentFacadeService {
    /**
     * 查询全部文档记录。
     *
     * @return 文档列表
     */
    GetDocumentsResponse getDocuments();

    /**
     * 查询某个知识库下的全部文档。
     *
     * @param kbId 知识库 ID
     * @return 指定知识库的文档列表
     */
    GetDocumentsResponse getDocumentsByKbId(String kbId);

    /**
     * 仅创建一条文档业务记录，不处理物理文件上传。
     *
     * @param request 文档创建请求
     * @return 新建文档 ID
     */
    CreateDocumentResponse createDocument(CreateDocumentRequest request);

    /**
     * 上传文档并完成持久化。
     * <p>
     * 当文件类型为 Markdown 时，还会继续触发解析、分块和向量化流程。
     *
     * @param kbId 目标知识库 ID
     * @param file 上传文件
     * @return 新建文档 ID
     */
    CreateDocumentResponse uploadDocument(String kbId, MultipartFile file);

    /**
     * 删除文档记录及其物理文件。
     *
     * @param documentId 文档 ID
     */
    void deleteDocument(String documentId);

    /**
     * 更新文档元数据。
     *
     * @param documentId 文档 ID
     * @param request 更新请求
     */
    void updateDocument(String documentId, UpdateDocumentRequest request);
}
