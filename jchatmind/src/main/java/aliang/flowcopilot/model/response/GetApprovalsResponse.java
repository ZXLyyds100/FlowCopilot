package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.ApprovalRecordVO;
import lombok.Builder;
import lombok.Data;

/**
 * Approval list response.
 */
@Data
@Builder
public class GetApprovalsResponse {
    private ApprovalRecordVO[] approvals;
}
