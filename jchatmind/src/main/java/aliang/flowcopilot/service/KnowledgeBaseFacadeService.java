package aliang.flowcopilot.service;

import aliang.flowcopilot.model.request.CreateKnowledgeBaseRequest;
import aliang.flowcopilot.model.request.UpdateKnowledgeBaseRequest;
import aliang.flowcopilot.model.response.CreateKnowledgeBaseResponse;
import aliang.flowcopilot.model.response.GetKnowledgeBasesResponse;

/**
 * 知识库门面服务。
 * <p>
 * 负责知识库的增删改查，并作为文档与向量数据的归属管理入口。
 */
public interface KnowledgeBaseFacadeService {
    /**
     * 查询全部知识库。
     *
     * @return 知识库列表
     */
    GetKnowledgeBasesResponse getKnowledgeBases();

    /**
     * 创建知识库。
     *
     * @param request 创建请求
     * @return 新建知识库 ID
     */
    CreateKnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request);

    /**
     * 删除知识库。
     *
     * @param knowledgeBaseId 知识库 ID
     */
    void deleteKnowledgeBase(String knowledgeBaseId);

    /**
     * 更新知识库信息。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param request 更新请求
     */
    void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request);
}
