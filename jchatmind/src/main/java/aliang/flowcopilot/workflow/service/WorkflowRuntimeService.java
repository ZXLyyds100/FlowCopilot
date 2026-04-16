package aliang.flowcopilot.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.ArtifactMapper;
import aliang.flowcopilot.mapper.WorkflowInstanceMapper;
import aliang.flowcopilot.mapper.WorkflowStepInstanceMapper;
import aliang.flowcopilot.model.entity.ApprovalRecord;
import aliang.flowcopilot.model.entity.Artifact;
import aliang.flowcopilot.model.entity.WorkflowInstance;
import aliang.flowcopilot.model.entity.WorkflowStepInstance;
import aliang.flowcopilot.model.request.CreateWorkflowRequest;
import aliang.flowcopilot.model.response.CreateWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowStepsResponse;
import aliang.flowcopilot.model.response.GetWorkflowsResponse;
import aliang.flowcopilot.model.vo.ArtifactVO;
import aliang.flowcopilot.model.vo.WorkflowInstanceVO;
import aliang.flowcopilot.model.vo.WorkflowStepInstanceVO;
import aliang.flowcopilot.workflow.event.WorkflowEventPublisher;
import aliang.flowcopilot.workflow.node.WorkflowNode;
import aliang.flowcopilot.workflow.state.ApprovalStatus;
import aliang.flowcopilot.workflow.state.WorkflowState;
import aliang.flowcopilot.workflow.state.WorkflowStatus;
import aliang.flowcopilot.workflow.state.WorkflowStepStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * First-stage fixed workflow runtime.
 * <p>
 * This is intentionally simple: it runs a fixed skeleton synchronously and
 * persists workflow instance, step records and final artifacts.
 */
@Service
@AllArgsConstructor
public class WorkflowRuntimeService {

    private final WorkflowInstanceMapper workflowInstanceMapper;
    private final WorkflowStepInstanceMapper workflowStepInstanceMapper;
    private final ArtifactMapper artifactMapper;
    private final ObjectMapper objectMapper;
    private final List<WorkflowNode> workflowNodes;
    private final Executor taskExecutor;
    private final ApprovalService approvalService;
    private final WorkflowEventPublisher workflowEventPublisher;

    public CreateWorkflowResponse createAndRun(CreateWorkflowRequest request) {
        if (!StringUtils.hasText(request.getInput())) {
            throw new BizException("工作流输入不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        WorkflowInstance workflowInstance = WorkflowInstance.builder()
                .title(resolveTitle(request))
                .input(request.getInput())
                .status(WorkflowStatus.CREATED.name())
                .currentStep("created")
                .metadata(toJson(new WorkflowMetadata(request.getKnowledgeBaseId())))
                .createdAt(now)
                .updatedAt(now)
                .build();
        workflowInstanceMapper.insert(workflowInstance);

        WorkflowState state = WorkflowState.builder()
                .workflowInstanceId(workflowInstance.getId())
                .title(workflowInstance.getTitle())
                .userInput(request.getInput())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .build();

        taskExecutor.execute(() -> runWorkflowSafely(workflowInstance, state, "planner"));

        return CreateWorkflowResponse.builder()
                .workflowInstanceId(workflowInstance.getId())
                .workflow(toVO(workflowInstance))
                .build();
    }

    public void approveAndResume(String approvalRecordId, String comment) {
        ApprovalRecord approval = approvalService.approve(approvalRecordId, comment);
        WorkflowInstance workflow = requireWorkflow(approval.getWorkflowInstanceId());
        WorkflowState state = loadLatestState(workflow);
        state.setApprovalRecordId(approval.getId());
        state.setApprovalStatus(ApprovalStatus.APPROVED.name());
        state.setApprovalComment(approval.getComment());
        state.setApprovalRequired(false);
        updateWorkflow(workflow.getId(), WorkflowStatus.RUNNING.name(), "publish", "审批通过，继续发布");
        workflowEventPublisher.workflowResumed(workflow.getId(), "审批通过，继续执行 Reporter Agent");
        taskExecutor.execute(() -> runWorkflowSafely(workflow, state, "publish"));
    }

    public void rejectAndRetry(String approvalRecordId, String comment) {
        ApprovalRecord approval = approvalService.reject(approvalRecordId, comment);
        WorkflowInstance workflow = requireWorkflow(approval.getWorkflowInstanceId());
        WorkflowState state = loadLatestState(workflow);
        state.setApprovalRecordId(approval.getId());
        state.setApprovalStatus(ApprovalStatus.REJECTED.name());
        state.setApprovalComment(approval.getComment());
        state.setApprovalRequired(false);
        updateWorkflow(workflow.getId(), WorkflowStatus.RUNNING.name(), "executor", "审批驳回，回退到 Executor Agent 重试");
        workflowEventPublisher.workflowResumed(workflow.getId(), "审批驳回，回退到 Executor Agent 重新生成");
        taskExecutor.execute(() -> runWorkflowSafely(workflow, state, "executor"));
    }

    public GetWorkflowResponse getWorkflow(String workflowInstanceId) {
        WorkflowInstance workflow = requireWorkflow(workflowInstanceId);
        List<WorkflowStepInstance> steps = workflowStepInstanceMapper.selectByWorkflowInstanceId(workflowInstanceId);
        List<Artifact> artifacts = artifactMapper.selectByWorkflowInstanceId(workflowInstanceId);
        return GetWorkflowResponse.builder()
                .workflow(toVO(workflow))
                .steps(steps.stream().map(this::toVO).toArray(WorkflowStepInstanceVO[]::new))
                .artifacts(artifacts.stream().map(this::toVO).toArray(ArtifactVO[]::new))
                .build();
    }

    public GetWorkflowStepsResponse getSteps(String workflowInstanceId) {
        requireWorkflow(workflowInstanceId);
        List<WorkflowStepInstance> steps = workflowStepInstanceMapper.selectByWorkflowInstanceId(workflowInstanceId);
        return GetWorkflowStepsResponse.builder()
                .steps(steps.stream().map(this::toVO).toArray(WorkflowStepInstanceVO[]::new))
                .build();
    }

    public GetWorkflowsResponse getRecentWorkflows(int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        List<WorkflowInstance> workflows = workflowInstanceMapper.selectRecent(safeLimit);
        return GetWorkflowsResponse.builder()
                .workflows(workflows.stream().map(this::toVO).toArray(WorkflowInstanceVO[]::new))
                .build();
    }

    private void runWorkflowSafely(WorkflowInstance workflowInstance, WorkflowState state, String startNodeKey) {
        try {
            runFixedSkeleton(workflowInstance, state, startNodeKey);
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            workflowEventPublisher.workflowFailed(workflowInstance.getId(), errorMessage);
        }
    }

    private void runFixedSkeleton(WorkflowInstance workflowInstance, WorkflowState state, String startNodeKey) {
        workflowEventPublisher.workflowStarted(workflowInstance.getId(), workflowInstance.getTitle());
        updateWorkflow(workflowInstance.getId(), WorkflowStatus.RUNNING.name(), "planner", null);
        for (WorkflowNode node : orderedNodesFrom(startNodeKey)) {
            WorkflowStepInstance step = startStep(workflowInstance.getId(), node, state);
            workflowEventPublisher.stepStarted(workflowInstance.getId(), step);
            try {
                updateWorkflow(workflowInstance.getId(), WorkflowStatus.RUNNING.name(), node.key(), null);
                WorkflowState outputState = node.execute(state);
                completeStep(step.getId(), outputState);
                state = outputState;
                WorkflowStepInstance completedStep = WorkflowStepInstance.builder()
                        .id(step.getId())
                        .workflowInstanceId(step.getWorkflowInstanceId())
                        .nodeKey(step.getNodeKey())
                        .nodeName(step.getNodeName())
                        .status(WorkflowStepStatus.COMPLETED.name())
                        .build();
                workflowEventPublisher.stepCompleted(workflowInstance.getId(), completedStep, summarizeNodeOutput(node.key(), state));
                if ("approval".equals(node.key()) && state.isApprovalRequired()) {
                    updateWorkflow(workflowInstance.getId(), WorkflowStatus.WAITING_APPROVAL.name(), "approval", state.getReviewComment());
                    workflowEventPublisher.approvalRequired(workflowInstance.getId(), state.getApprovalRecordId(), state.getReviewComment());
                    return;
                }
            } catch (Exception e) {
                String errorMessage = resolveErrorMessage(e);
                failStep(step.getId(), e);
                updateWorkflow(workflowInstance.getId(), WorkflowStatus.FAILED.name(), node.key(), errorMessage);
                workflowEventPublisher.stepFailed(workflowInstance.getId(), step, errorMessage);
                throw new BizException("工作流节点执行失败: " + node.name() + ", " + errorMessage);
            }
        }
        updateWorkflow(workflowInstance.getId(), WorkflowStatus.COMPLETED.name(), "finished", state.getFinalOutput());
        workflowEventPublisher.workflowFinished(workflowInstance.getId(), state.getFinalOutput());
    }

    private String resolveErrorMessage(Exception e) {
        if (StringUtils.hasText(e.getMessage())) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private List<WorkflowNode> orderedNodes() {
        List<String> order = List.of("planner", "retriever", "executor", "reviewer", "approval", "publish");
        return workflowNodes.stream()
                .filter(node -> order.contains(node.key()))
                .sorted(Comparator.comparingInt(node -> order.indexOf(node.key())))
                .toList();
    }

    private List<WorkflowNode> orderedNodesFrom(String startNodeKey) {
        List<WorkflowNode> nodes = orderedNodes();
        int startIndex = 0;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).key().equals(startNodeKey)) {
                startIndex = i;
                break;
            }
        }
        return nodes.subList(startIndex, nodes.size());
    }

    private WorkflowState loadLatestState(WorkflowInstance workflow) {
        List<WorkflowStepInstance> steps = workflowStepInstanceMapper.selectByWorkflowInstanceId(workflow.getId());
        for (int i = steps.size() - 1; i >= 0; i--) {
            String snapshot = steps.get(i).getOutputSnapshot();
            if (StringUtils.hasText(snapshot) && !"{}".equals(snapshot)) {
                try {
                    return objectMapper.readValue(snapshot, WorkflowState.class);
                } catch (JsonProcessingException ignored) {
                    break;
                }
            }
        }
        return WorkflowState.builder()
                .workflowInstanceId(workflow.getId())
                .title(workflow.getTitle())
                .userInput(workflow.getInput())
                .build();
    }

    private String summarizeNodeOutput(String nodeKey, WorkflowState state) {
        return switch (nodeKey) {
            case "planner" -> state.getPlan();
            case "retriever" -> String.join("\n", state.safeRetrievedContents());
            case "executor" -> state.getDraftResult();
            case "reviewer" -> state.getReviewComment();
            case "approval" -> "等待人工审批：" + state.getApprovalRecordId();
            case "publish" -> state.getFinalOutput();
            default -> state.getFinalOutput();
        };
    }

    private WorkflowStepInstance startStep(String workflowInstanceId, WorkflowNode node, WorkflowState state) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowStepInstance step = WorkflowStepInstance.builder()
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(node.key())
                .nodeName(node.name())
                .status(WorkflowStepStatus.RUNNING.name())
                .inputSnapshot(toJson(state))
                .outputSnapshot("{}")
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        workflowStepInstanceMapper.insert(step);
        return step;
    }

    private void completeStep(String stepId, WorkflowState state) {
        WorkflowStepInstance step = WorkflowStepInstance.builder()
                .id(stepId)
                .status(WorkflowStepStatus.COMPLETED.name())
                .outputSnapshot(toJson(state))
                .completedAt(LocalDateTime.now())
                .build();
        workflowStepInstanceMapper.updateById(step);
    }

    private void failStep(String stepId, Exception e) {
        WorkflowStepInstance step = WorkflowStepInstance.builder()
                .id(stepId)
                .status(WorkflowStepStatus.FAILED.name())
                .errorMessage(e.getMessage())
                .completedAt(LocalDateTime.now())
                .build();
        workflowStepInstanceMapper.updateById(step);
    }

    private void updateWorkflow(String workflowInstanceId, String status, String currentStep, String result) {
        WorkflowInstance update = WorkflowInstance.builder()
                .id(workflowInstanceId)
                .status(status)
                .currentStep(currentStep)
                .result(result)
                .build();
        workflowInstanceMapper.updateById(update);
    }

    private WorkflowInstance requireWorkflow(String workflowInstanceId) {
        WorkflowInstance workflow = workflowInstanceMapper.selectById(workflowInstanceId);
        if (workflow == null) {
            throw new BizException("工作流实例不存在: " + workflowInstanceId);
        }
        return workflow;
    }

    private String resolveTitle(CreateWorkflowRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle();
        }
        String input = request.getInput().trim();
        return input.length() > 30 ? input.substring(0, 30) : input;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("工作流状态序列化失败: " + e.getMessage());
        }
    }

    private WorkflowInstanceVO toVO(WorkflowInstance workflow) {
        return WorkflowInstanceVO.builder()
                .id(workflow.getId())
                .definitionId(workflow.getDefinitionId())
                .title(workflow.getTitle())
                .input(workflow.getInput())
                .status(workflow.getStatus())
                .currentStep(workflow.getCurrentStep())
                .result(workflow.getResult())
                .metadata(workflow.getMetadata())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }

    private WorkflowStepInstanceVO toVO(WorkflowStepInstance step) {
        return WorkflowStepInstanceVO.builder()
                .id(step.getId())
                .workflowInstanceId(step.getWorkflowInstanceId())
                .nodeKey(step.getNodeKey())
                .nodeName(step.getNodeName())
                .status(step.getStatus())
                .inputSnapshot(step.getInputSnapshot())
                .outputSnapshot(step.getOutputSnapshot())
                .errorMessage(step.getErrorMessage())
                .startedAt(step.getStartedAt())
                .completedAt(step.getCompletedAt())
                .createdAt(step.getCreatedAt())
                .updatedAt(step.getUpdatedAt())
                .build();
    }

    private ArtifactVO toVO(Artifact artifact) {
        return ArtifactVO.builder()
                .id(artifact.getId())
                .workflowInstanceId(artifact.getWorkflowInstanceId())
                .type(artifact.getType())
                .title(artifact.getTitle())
                .content(artifact.getContent())
                .metadata(artifact.getMetadata())
                .createdAt(artifact.getCreatedAt())
                .updatedAt(artifact.getUpdatedAt())
                .build();
    }

    private record WorkflowMetadata(String knowledgeBaseId) {
    }
}
