package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.DocumentVO;
import lombok.Builder;
import lombok.Data;

/**
 * 文档列表响应对象。
 */
@Data
@Builder
public class GetDocumentsResponse {
    private DocumentVO[] documents;
}
