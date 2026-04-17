package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文档前端展示对象。
 */
@Data
@Builder
public class DocumentVO {
    private String id;
    private String kbId;
    private String filename;
    private String filetype;
    private Long size;
}
