package com.kama.jchatmind.model.response;

import com.kama.jchatmind.model.vo.DocumentVO;
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
