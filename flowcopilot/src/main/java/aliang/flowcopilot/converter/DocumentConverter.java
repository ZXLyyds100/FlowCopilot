package aliang.flowcopilot.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import aliang.flowcopilot.model.dto.DocumentDTO;
import aliang.flowcopilot.model.entity.Document;
import aliang.flowcopilot.model.request.CreateDocumentRequest;
import aliang.flowcopilot.model.request.UpdateDocumentRequest;
import aliang.flowcopilot.model.vo.DocumentVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 文档对象转换器。
 * <p>
 * 负责文档在 Request、DTO、Entity、VO 之间的转换，并处理文件元数据的 JSON 字段。
 */
@Component
@AllArgsConstructor
public class DocumentConverter {

    private final ObjectMapper objectMapper;

    /**
     * 将文档 DTO 转为数据库实体。
     *
     * @param documentDTO 文档 DTO
     * @return 文档实体
     * @throws JsonProcessingException 元数据序列化失败时抛出
     */
    public Document toEntity(DocumentDTO documentDTO) throws JsonProcessingException {
        Assert.notNull(documentDTO, "DocumentDTO cannot be null");

        return Document.builder()
                .id(documentDTO.getId())
                .kbId(documentDTO.getKbId())
                .filename(documentDTO.getFilename())
                .filetype(documentDTO.getFiletype())
                .size(documentDTO.getSize())
                .metadata(documentDTO.getMetadata() != null 
                        ? objectMapper.writeValueAsString(documentDTO.getMetadata()) 
                        : null)
                .createdAt(documentDTO.getCreatedAt())
                .updatedAt(documentDTO.getUpdatedAt())
                .build();
    }

    /**
     * 将数据库实体转为文档 DTO。
     *
     * @param document 文档实体
     * @return 文档 DTO
     * @throws JsonProcessingException 元数据反序列化失败时抛出
     */
    public DocumentDTO toDTO(Document document) throws JsonProcessingException {
        Assert.notNull(document, "Document cannot be null");

        return DocumentDTO.builder()
                .id(document.getId())
                .kbId(document.getKbId())
                .filename(document.getFilename())
                .filetype(document.getFiletype())
                .size(document.getSize())
                .metadata(document.getMetadata() != null 
                        ? objectMapper.readValue(document.getMetadata(), DocumentDTO.MetaData.class) 
                        : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * 将文档 DTO 转为前端展示对象。
     *
     * @param dto 文档 DTO
     * @return 文档 VO
     */
    public DocumentVO toVO(DocumentDTO dto) {
        return DocumentVO.builder()
                .id(dto.getId())
                .kbId(dto.getKbId())
                .filename(dto.getFilename())
                .filetype(dto.getFiletype())
                .size(dto.getSize())
                .build();
    }

    /**
     * 将文档实体直接转换为前端展示对象。
     *
     * @param document 文档实体
     * @return 文档 VO
     * @throws JsonProcessingException JSON 转换失败时抛出
     */
    public DocumentVO toVO(Document document) throws JsonProcessingException {
        return toVO(toDTO(document));
    }

    /**
     * 将创建文档请求转换为 DTO。
     *
     * @param request 创建请求
     * @return 文档 DTO
     */
    public DocumentDTO toDTO(CreateDocumentRequest request) {
        Assert.notNull(request, "CreateDocumentRequest cannot be null");
        Assert.notNull(request.getKbId(), "KbId cannot be null");

        return DocumentDTO.builder()
                .kbId(request.getKbId())
                .filename(request.getFilename())
                .filetype(request.getFiletype())
                .size(request.getSize())
                .build();
    }

    /**
     * 用更新请求中的非空字段覆盖 DTO。
     *
     * @param dto 目标 DTO
     * @param request 更新请求
     */
    public void updateDTOFromRequest(DocumentDTO dto, UpdateDocumentRequest request) {
        Assert.notNull(dto, "DocumentDTO cannot be null");
        Assert.notNull(request, "UpdateDocumentRequest cannot be null");

        if (request.getFilename() != null) {
            dto.setFilename(request.getFilename());
        }
        if (request.getFiletype() != null) {
            dto.setFiletype(request.getFiletype());
        }
        if (request.getSize() != null) {
            dto.setSize(request.getSize());
        }
    }
}
