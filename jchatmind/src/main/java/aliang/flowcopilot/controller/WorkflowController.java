package aliang.flowcopilot.controller;

import aliang.flowcopilot.model.common.ApiResponse;
import aliang.flowcopilot.model.request.CreateWorkflowRequest;
import aliang.flowcopilot.model.response.CreateWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowStepsResponse;
import aliang.flowcopilot.model.response.GetWorkflowsResponse;
import aliang.flowcopilot.workflow.service.WorkflowRuntimeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Workflow API for the first-stage FlowCopilot execution loop.
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

    @GetMapping("/{workflowInstanceId}")
    public ApiResponse<GetWorkflowResponse> getWorkflow(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getWorkflow(workflowInstanceId));
    }

    @GetMapping("/{workflowInstanceId}/steps")
    public ApiResponse<GetWorkflowStepsResponse> getWorkflowSteps(@PathVariable String workflowInstanceId) {
        return ApiResponse.success(workflowRuntimeService.getSteps(workflowInstanceId));
    }
}
