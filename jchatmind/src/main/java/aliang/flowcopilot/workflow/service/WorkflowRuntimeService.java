package aliang.flowcopilot.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.ArtifactMapper;
import aliang.flowcopilot.mapper.WorkflowInstanceMapper;
import aliang.flowcopilot.mapper.WorkflowStepInstanceMapper;
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
import aliang.flowcopilot.workflow.node.WorkflowNode;
import aliang.flowcopilot.workflow.state.WorkflowState;
import aliang.flowcopilot.workflow.state.WorkflowStatus;
import aliang.flowcopilot.workflow.state.WorkflowStepStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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

        runFixedSkeleton(workflowInstance, state);

        WorkflowInstance completed = workflowInstanceMapper.selectById(workflowInstance.getId());
        return CreateWorkflowResponse.builder()
                .workflowInstanceId(workflowInstance.getId())
                .workflow(toVO(completed))
                .build();
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

    private void runFixedSkeleton(WorkflowInstance workflowInstance, WorkflowState state) {
        updateWorkflow(workflowInstance.getId(), WorkflowStatus.RUNNING.name(), "planner", null);
        for (WorkflowNode node : orderedNodes()) {
            WorkflowStepInstance step = startStep(workflowInstance.getId(), node, state);
            try {
                updateWorkflow(workflowInstance.getId(), WorkflowStatus.RUNNING.name(), node.key(), null);
                WorkflowState outputState = node.execute(state);
                completeStep(step.getId(), outputState);
                state = outputState;
            } catch (Exception e) {
                failStep(step.getId(), e);
                updateWorkflow(workflowInstance.getId(), WorkflowStatus.FAILED.name(), node.key(), e.getMessage());
                throw new BizException("工作流节点执行失败: " + node.name() + ", " + e.getMessage());
            }
        }
        updateWorkflow(workflowInstance.getId(), WorkflowStatus.COMPLETED.name(), "finished", state.getFinalOutput());
    }

    private List<WorkflowNode> orderedNodes() {
        List<String> order = List.of("planner", "retriever", "executor", "publish");
        return workflowNodes.stream()
                .filter(node -> order.contains(node.key()))
                .sorted(Comparator.comparingInt(node -> order.indexOf(node.key())))
                .toList();
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
