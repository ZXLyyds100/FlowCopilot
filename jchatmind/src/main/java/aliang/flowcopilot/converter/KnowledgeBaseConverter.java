package aliang.flowcopilot.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import aliang.flowcopilot.model.dto.KnowledgeBaseDTO;
import aliang.flowcopilot.model.entity.KnowledgeBase;
import aliang.flowcopilot.model.request.CreateKnowledgeBaseRequest;
import aliang.flowcopilot.model.request.UpdateKnowledgeBaseRequest;
import aliang.flowcopilot.model.vo.KnowledgeBaseVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 知识库对象转换器。
 * <p>
 * 负责知识库在 Request、DTO、Entity、VO 之间的互转，并处理元数据 JSON 字段。
 */
@Component
@AllArgsConstructor
public class KnowledgeBaseConverter {

    private final ObjectMapper objectMapper;

    /**
     * 将知识库 DTO 转换为数据库实体。
     *
     * @param knowledgeBaseDTO 知识库 DTO
     * @return 知识库实体
     * @throws JsonProcessingException 元数据序列化失败时抛出
     */
    public KnowledgeBase toEntity(KnowledgeBaseDTO knowledgeBaseDTO) throws JsonProcessingException {
        Assert.notNull(knowledgeBaseDTO, "KnowledgeBaseDTO cannot be null");

        return KnowledgeBase.builder()
                .id(knowledgeBaseDTO.getId())
                .name(knowledgeBaseDTO.getName())
                .description(knowledgeBaseDTO.getDescription())
                .metadata(knowledgeBaseDTO.getMetadata() != null 
                        ? objectMapper.writeValueAsString(knowledgeBaseDTO.getMetadata()) 
                        : null)
                .createdAt(knowledgeBaseDTO.getCreatedAt())
                .updatedAt(knowledgeBaseDTO.getUpdatedAt())
                .build();
    }

    /**
     * 将数据库实体转换为知识库 DTO。
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库 DTO
     * @throws JsonProcessingException 元数据反序列化失败时抛出
     */
    public KnowledgeBaseDTO toDTO(KnowledgeBase knowledgeBase) throws JsonProcessingException {
        Assert.notNull(knowledgeBase, "KnowledgeBase cannot be null");

        return KnowledgeBaseDTO.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .metadata(knowledgeBase.getMetadata() != null 
                        ? objectMapper.readValue(knowledgeBase.getMetadata(), KnowledgeBaseDTO.MetaData.class) 
                        : null)
                .createdAt(knowledgeBase.getCreatedAt())
                .updatedAt(knowledgeBase.getUpdatedAt())
                .build();
    }

    /**
     * 将知识库 DTO 转为前端展示对象。
     *
     * @param dto 知识库 DTO
     * @return 知识库 VO
     */
    public KnowledgeBaseVO toVO(KnowledgeBaseDTO dto) {
        return KnowledgeBaseVO.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    /**
     * 将知识库实体直接转为前端展示对象。
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库 VO
     * @throws JsonProcessingException JSON 转换失败时抛出
     */
    public KnowledgeBaseVO toVO(KnowledgeBase knowledgeBase) throws JsonProcessingException {
        return toVO(toDTO(knowledgeBase));
    }

    /**
     * 将创建知识库请求转换为 DTO。
     *
     * @param request 创建请求
     * @return 知识库 DTO
     */
    public KnowledgeBaseDTO toDTO(CreateKnowledgeBaseRequest request) {
        Assert.notNull(request, "CreateKnowledgeBaseRequest cannot be null");

        return KnowledgeBaseDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * 用更新请求中的非空字段覆盖 DTO。
     *
     * @param dto 目标 DTO
     * @param request 更新请求
     */
    public void updateDTOFromRequest(KnowledgeBaseDTO dto, UpdateKnowledgeBaseRequest request) {
        Assert.notNull(dto, "KnowledgeBaseDTO cannot be null");
        Assert.notNull(request, "UpdateKnowledgeBaseRequest cannot be null");

        if (request.getName() != null) {
            dto.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dto.setDescription(request.getDescription());
        }
    }
}
