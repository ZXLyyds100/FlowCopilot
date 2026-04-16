package aliang.flowcopilot.controller;

import aliang.flowcopilot.model.common.ApiResponse;
import aliang.flowcopilot.model.request.CreateWorkflowRequest;
import aliang.flowcopilot.model.request.UpdateWorkflowTemplateRequest;
import aliang.flowcopilot.model.response.CreateWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowCheckpointsResponse;
import aliang.flowcopilot.model.response.GetWorkflowObservabilityResponse;
import aliang.flowcopilot.model.response.GetWorkflowTemplatesResponse;
import aliang.flowcopilot.model.response.GetWorkflowTraceResponse;
import aliang.flowcopilot.model.response.GetWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowStepsResponse;
import aliang.flowcopilot.model.response.GetWorkflowsResponse;
import aliang.flowcopilot.model.vo.WorkflowTemplateVO;
import aliang.flowcopilot.workflow.service.WorkflowRuntimeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Workflow API for the FlowCopilot execution loop.
 */
@RestController
@RequestMapping("/api/workflows")
@AllArgsConstructor
public class WorkflowController {

    private final WorkflowRuntimeService workflowRuntimeService;

    @PostMapping
    public ApiResponse<CreateWorkflowResponse> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        return ApiResponse.success(workflowRuntimeService.createAndRun(request));
    }

    @GetMapping
    public ApiResponse<GetWorkflowsResponse> getRecentWorkflows(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(workflowRuntimeService.getRecentWorkflows(limit));
    }

    @GetMapping("/templates")
    public ApiResponse<GetWorkflowTemplatesResponse> getWorkflowTemplates() {
        return ApiResponse.success(workflowRuntimeService.getTemplates());
    }

    @PutMapping("/templates/{templateCode}")
    public ApiResponse<WorkflowTemplateVO> updateWorkflowTemplate(
            @PathVariable String templateCode,
            @RequestBody UpdateWorkflowTemplateRequest request
    ) {
        return ApiResponse.success(workflowRuntimeService.updateTemplate(templateCode, request));
    }

    @GetMapping("/{workflowInstanceId}")
    public ApiResponse<GetWorkflowResponse> getWorkflow(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getWorkflow(workflowInstanceId));
    }

    @GetMapping("/{workflowInstanceId}/steps")
    public ApiResponse<GetWorkflowStepsResponse> getWorkflowSteps(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getSteps(workflowInstanceId));
    }

    @GetMapping("/{workflowInstanceId}/trace")
    public ApiResponse<GetWorkflowTraceResponse> getWorkflowTrace(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getTrace(workflowInstanceId));
    }

    @GetMapping("/{workflowInstanceId}/checkpoints")
    public ApiResponse<GetWorkflowCheckpointsResponse> getWorkflowCheckpoints(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getCheckpoints(workflowInstanceId));
    }

    @GetMapping("/{workflowInstanceId}/observability")
    public ApiResponse<GetWorkflowObservabilityResponse> getWorkflowObservability(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getObservability(workflowInstanceId));
    }

    @PostMapping("/{workflowInstanceId}/replay/{nodeKey}")
    public ApiResponse<Void> replayFromNode(
            @PathVariable String workflowInstanceId,
            @PathVariable String nodeKey
    ) {
        workflowRuntimeService.replayFrom(workflowInstanceId, nodeKey);
        return ApiResponse.success();
    }
}
