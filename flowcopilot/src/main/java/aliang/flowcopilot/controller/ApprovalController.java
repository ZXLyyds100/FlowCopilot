package aliang.flowcopilot.controller;

import aliang.flowcopilot.model.common.ApiResponse;
import aliang.flowcopilot.model.request.ApprovalDecisionRequest;
import aliang.flowcopilot.model.response.GetApprovalsResponse;
import aliang.flowcopilot.model.vo.ApprovalRecordVO;
import aliang.flowcopilot.workflow.service.ApprovalService;
import aliang.flowcopilot.workflow.service.WorkflowRuntimeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Human approval API for third-stage workflow pause/resume.
 */
@RestController
@RequestMapping("/api/approvals")
@AllArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final WorkflowRuntimeService workflowRuntimeService;

    @GetMapping
    public ApiResponse<GetApprovalsResponse> getApprovals(
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        return ApiResponse.success(approvalService.getApprovals(status));
    }

    @PostMapping("/{approvalRecordId}/approve")
    public ApiResponse<ApprovalRecordVO> approve(
            @PathVariable String approvalRecordId,
            @RequestBody(required = false) ApprovalDecisionRequest request
    ) {
        String comment = request == null ? null : request.getComment();
        workflowRuntimeService.approveAndResume(approvalRecordId, comment);
        return ApiResponse.success(approvalService.toVO(approvalService.requireApproval(approvalRecordId)));
    }

    @PostMapping("/{approvalRecordId}/reject")
    public ApiResponse<ApprovalRecordVO> reject(
            @PathVariable String approvalRecordId,
            @RequestBody(required = false) ApprovalDecisionRequest request
    ) {
        String comment = request == null ? null : request.getComment();
        workflowRuntimeService.rejectAndRetry(approvalRecordId, comment);
        return ApiResponse.success(approvalService.toVO(approvalService.requireApproval(approvalRecordId)));
    }
}
