package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.model.entity.ApprovalRecord;
import aliang.flowcopilot.workflow.service.ApprovalService;
import aliang.flowcopilot.workflow.state.ApprovalStatus;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pauses the workflow until a human approves or rejects the current draft.
 */
@Component
@AllArgsConstructor
public class ApprovalNode implements WorkflowNode {

    private final ApprovalService approvalService;

    @Override
    public String key() {
        return "approval";
    }

    @Override
    public String name() {
        return "Human Approval";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        ApprovalRecord approvalRecord = approvalService.createPendingApproval(state);
        state.setApprovalRecordId(approvalRecord.getId());
        state.setApprovalStatus(ApprovalStatus.PENDING.name());
        state.setApprovalRequired(true);
        return state;
    }
}
