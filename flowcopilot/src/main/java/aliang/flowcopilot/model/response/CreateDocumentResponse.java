package aliang.flowcopilot.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 创建文档响应对象。
 */
@Data
@Builder
public class CreateDocumentResponse {
    private String documentId;
}
