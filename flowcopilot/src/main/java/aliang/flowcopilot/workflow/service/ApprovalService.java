package aliang.flowcopilot.workflow.service;

import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.ApprovalRecordMapper;
import aliang.flowcopilot.model.entity.ApprovalRecord;
import aliang.flowcopilot.model.response.GetApprovalsResponse;
import aliang.flowcopilot.model.vo.ApprovalRecordVO;
import aliang.flowcopilot.workflow.state.ApprovalStatus;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles human approval records for paused workflows.
 */
@Service
@AllArgsConstructor
public class ApprovalService {

    private final ApprovalRecordMapper approvalRecordMapper;

    public ApprovalRecord createPendingApproval(WorkflowState state) {
        ApprovalRecord existing = approvalRecordMapper.selectPendingByWorkflowInstanceId(state.getWorkflowInstanceId());
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        ApprovalRecord record = ApprovalRecord.builder()
                .workflowInstanceId(state.getWorkflowInstanceId())
                .status(ApprovalStatus.PENDING.name())
                .title("审批：" + state.getTitle())
                .summary(buildSummary(state))
                .createdAt(now)
                .updatedAt(now)
                .build();
        approvalRecordMapper.insert(record);
        return record;
    }

    public ApprovalRecord approve(String approvalRecordId, String comment) {
        ApprovalRecord record = requireApproval(approvalRecordId);
        if (!ApprovalStatus.PENDING.name().equals(record.getStatus())) {
            throw new BizException("审批记录已处理，不能重复审批");
        }
        ApprovalRecord update = ApprovalRecord.builder()
                .id(approvalRecordId)
                .status(ApprovalStatus.APPROVED.name())
                .comment(comment)
                .decidedAt(LocalDateTime.now())
                .build();
        approvalRecordMapper.updateById(update);
        return requireApproval(approvalRecordId);
    }

    public ApprovalRecord reject(String approvalRecordId, String comment) {
        ApprovalRecord record = requireApproval(approvalRecordId);
        if (!ApprovalStatus.PENDING.name().equals(record.getStatus())) {
            throw new BizException("审批记录已处理，不能重复驳回");
        }
        ApprovalRecord update = ApprovalRecord.builder()
                .id(approvalRecordId)
                .status(ApprovalStatus.REJECTED.name())
                .comment(comment)
                .decidedAt(LocalDateTime.now())
                .build();
        approvalRecordMapper.updateById(update);
        return requireApproval(approvalRecordId);
    }

    public GetApprovalsResponse getApprovals(String status) {
        String safeStatus = status == null || status.isBlank() ? ApprovalStatus.PENDING.name() : status;
        List<ApprovalRecord> records = approvalRecordMapper.selectByStatus(safeStatus);
        return GetApprovalsResponse.builder()
                .approvals(records.stream().map(this::toVO).toArray(ApprovalRecordVO[]::new))
                .build();
    }

    public ApprovalRecord requireApproval(String approvalRecordId) {
        ApprovalRecord record = approvalRecordMapper.selectById(approvalRecordId);
        if (record == null) {
            throw new BizException("审批记录不存在: " + approvalRecordId);
        }
        return record;
    }

    public ApprovalRecordVO toVO(ApprovalRecord record) {
        return ApprovalRecordVO.builder()
                .id(record.getId())
                .workflowInstanceId(record.getWorkflowInstanceId())
                .status(record.getStatus())
                .title(record.getTitle())
                .summary(record.getSummary())
                .comment(record.getComment())
                .decidedAt(record.getDecidedAt())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private String buildSummary(WorkflowState state) {
        return """
                任务：%s

                Reviewer 结论：
                %s

                请确认是否允许进入最终发布阶段。若驳回，流程会回退到 Executor Agent 根据审批意见重新生成。
                """.formatted(state.getUserInput(), state.getReviewComment()).strip();
    }
}
