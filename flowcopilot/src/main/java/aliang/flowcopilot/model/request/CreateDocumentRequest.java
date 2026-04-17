package aliang.flowcopilot.model.request;

import lombok.Data;

/**
 * 创建文档记录请求对象。
 */
@Data
public class CreateDocumentRequest {
    private String kbId;
    private String filename;
    private String filetype;
    private Long size;
}
