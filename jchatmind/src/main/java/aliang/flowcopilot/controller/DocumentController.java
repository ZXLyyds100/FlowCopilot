package aliang.flowcopilot.controller;

import aliang.flowcopilot.model.common.ApiResponse;
import aliang.flowcopilot.model.request.CreateDocumentRequest;
import aliang.flowcopilot.model.request.UpdateDocumentRequest;
import aliang.flowcopilot.model.response.CreateDocumentResponse;
import aliang.flowcopilot.model.response.GetDocumentsResponse;
import aliang.flowcopilot.service.DocumentFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档控制器。
 * <p>
 * 负责文档记录查询、文件上传以及文档元数据维护。
 * 文档上传接口还是知识库向量化入库流程的起点。
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class DocumentController {

    private final DocumentFacadeService documentFacadeService;

    /**
     * 查询全部文档记录。
     *
     * @return 文档列表
     */
    @GetMapping("/documents")
    public ApiResponse<GetDocumentsResponse> getDocuments() {
        return ApiResponse.success(documentFacadeService.getDocuments());
    }

    /**
     * 查询指定知识库下的文档。
     *
     * @param kbId 知识库 ID
     * @return 文档列表
     */
    @GetMapping("/documents/kb/{kbId}")
    public ApiResponse<GetDocumentsResponse> getDocumentsByKbId(@PathVariable String kbId) {
        return ApiResponse.success(documentFacadeService.getDocumentsByKbId(kbId));
    }

    /**
     * 仅创建文档业务记录，不上传文件内容。
     *
     * @param request 文档创建请求
     * @return 新建文档 ID
     */
    @PostMapping("/documents")
    public ApiResponse<CreateDocumentResponse> createDocument(@RequestBody CreateDocumentRequest request) {
        return ApiResponse.success(documentFacadeService.createDocument(request));
    }

    /**
     * 上传文档并创建记录。
     * <p>
     * 当上传 Markdown 文件时，业务层还会继续触发解析、切块和向量化。
     *
     * @param kbId 知识库 ID
     * @param file 上传文件
     * @return 新建文档 ID
     */
    @PostMapping("/documents/upload")
    public ApiResponse<CreateDocumentResponse> uploadDocument(
            @RequestParam("kbId") String kbId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentFacadeService.uploadDocument(kbId, file));
    }

    /**
     * 删除指定文档。
     *
     * @param documentId 文档 ID
     * @return 空成功响应
     */
    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable String documentId) {
        documentFacadeService.deleteDocument(documentId);
        return ApiResponse.success();
    }

    /**
     * 更新文档元数据。
     *
     * @param documentId 文档 ID
     * @param request 更新请求
     * @return 空成功响应
     */
    @PatchMapping("/documents/{documentId}")
    public ApiResponse<Void> updateDocument(@PathVariable String documentId, @RequestBody UpdateDocumentRequest request) {
        documentFacadeService.updateDocument(documentId, request);
        return ApiResponse.success();
    }
}
