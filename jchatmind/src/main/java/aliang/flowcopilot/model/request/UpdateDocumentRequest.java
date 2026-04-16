package aliang.flowcopilot.model.request;

import lombok.Data;

/**
 * 更新文档请求对象。
 */
@Data
public class UpdateDocumentRequest {
    private String filename;
    private String filetype;
    private Long size;
}
