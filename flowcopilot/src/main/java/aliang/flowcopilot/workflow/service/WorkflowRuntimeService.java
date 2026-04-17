package aliang.flowcopilot.workflow.service;

import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.ArtifactMapper;
import aliang.flowcopilot.mapper.ExecutionTraceRefMapper;
import aliang.flowcopilot.mapper.WorkflowExecutionCheckpointMapper;
import aliang.flowcopilot.mapper.WorkflowInstanceMapper;
import aliang.flowcopilot.mapper.WorkflowStepInstanceMapper;
import aliang.flowcopilot.model.entity.ApprovalRecord;
import aliang.flowcopilot.model.entity.Artifact;
import aliang.flowcopilot.model.entity.ExecutionTraceRef;
import aliang.flowcopilot.model.entity.WorkflowDefinition;
import aliang.flowcopilot.model.entity.WorkflowExecutionCheckpoint;
import aliang.flowcopilot.model.entity.WorkflowInstance;
import aliang.flowcopilot.model.entity.WorkflowStepInstance;
import aliang.flowcopilot.model.request.CreateWorkflowRequest;
import aliang.flowcopilot.model.request.UpdateWorkflowTemplateRequest;
import aliang.flowcopilot.model.response.CreateWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowCheckpointsResponse;
import aliang.flowcopilot.model.response.GetWorkflowObservabilityResponse;
import aliang.flowcopilot.model.response.GetWorkflowResponse;
import aliang.flowcopilot.model.response.GetWorkflowStepsResponse;
import aliang.flowcopilot.model.response.GetWorkflowTemplatesResponse;
import aliang.flowcopilot.model.response.GetWorkflowTraceResponse;
import aliang.flowcopilot.model.response.GetWorkflowsResponse;
import aliang.flowcopilot.model.vo.ArtifactVO;
import aliang.flowcopilot.model.vo.ExecutionTraceRefVO;
import aliang.flowcopilot.model.vo.WorkflowExecutionCheckpointVO;
import aliang.flowcopilot.model.vo.WorkflowInstanceVO;
import aliang.flowcopilot.model.vo.WorkflowStepInstanceVO;
import aliang.flowcopilot.model.vo.WorkflowTemplateVO;
import aliang.flowcopilot.workflow.event.WorkflowEventPublisher;
import aliang.flowcopilot.workflow.graph.LangGraph4jGraphCompiler;
import aliang.flowcopilot.workflow.graph.WorkflowGraphDefinition;
import aliang.flowcopilot.workflow.graph.WorkflowGraphNode;
import aliang.flowcopilot.workflow.graph.WorkflowGraphRegistry;
import aliang.flowcopilot.workflow.graph.WorkflowSubGraphDefinition;
import aliang.flowcopilot.workflow.graph.WorkflowSubGraphGroup;
import aliang.flowcopilot.workflow.graph.WorkflowSubGraphStep;
import aliang.flowcopilot.workflow.node.WorkflowNode;
import aliang.flowcopilot.workflow.observability.ObservationRecorder;
import aliang.flowcopilot.workflow.observability.ObservationScope;
import aliang.flowcopilot.workflow.observability.ObservationStatus;
import aliang.flowcopilot.workflow.observability.WorkflowObservabilityService;
import aliang.flowcopilot.workflow.state.ApprovalStatus;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import aliang.flowcopilot.workflow.state.WorkflowState;
import aliang.flowcopilot.workflow.state.WorkflowStatus;
import aliang.flowcopilot.workflow.state.WorkflowStepStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Runtime orchestrator for workflow execution, replay, checkpoints and template operations.
 */
@Service
@AllArgsConstructor
public class WorkflowRuntimeService {

    private static final String CHECKPOINT_WORKFLOW_STARTED = "WORKFLOW_STARTED";
    private static final String CHECKPOINT_BEFORE_NODE = "BEFORE_NODE";
    private static final String CHECKPOINT_AFTER_NODE = "AFTER_NODE";
    private static final String CHECKPOINT_WAITING_APPROVAL = "WAITING_APPROVAL";
    private static final String CHECKPOINT_FAILED_NODE = "FAILED_NODE";
    private static final String CHECKPOINT_WORKFLOW_COMPLETED = "WORKFLOW_COMPLETED";
    private static final String CHECKPOINT_SUBGRAPH_STEP = "SUBGRAPH_STEP";
    private static final String WORKFLOW_CHECKPOINT_NODE = "__workflow__";

    private final WorkflowInstanceMapper workflowInstanceMapper;
    private final WorkflowStepInstanceMapper workflowStepInstanceMapper;
    private final ExecutionTraceRefMapper executionTraceRefMapper;
    private final ArtifactMapper artifactMapper;
    private final WorkflowExecutionCheckpointMapper workflowExecutionCheckpointMapper;
    private final ObjectMapper objectMapper;
    private final List<WorkflowNode> workflowNodes;
    private final Executor taskExecutor;
    private final ApprovalService approvalService;
    private final WorkflowEventPublisher workflowEventPublisher;
    private final WorkflowGraphRegistry workflowGraphRegistry;
    private final LangGraph4jGraphCompiler langGraph4jGraphCompiler;
    private final ObservationRecorder observationRecorder;
    private final WorkflowObservabilityService workflowObservabilityService;
    private final WorkflowSubGraphService workflowSubGraphService;

    public CreateWorkflowResponse createAndRun(CreateWorkflowRequest request) {
        if (!StringUtils.hasText(request.getInput())) {
            throw new BizException("Workflow input must not be blank");
        }

        String templateCode = workflowGraphRegistry.resolveTemplateCode(request.getTemplateCode());
        WorkflowDefinition storedDefinition = workflowGraphRegistry.getStoredDefinition(templateCode);
        LocalDateTime now = LocalDateTime.now();

        WorkflowInstance workflowInstance = WorkflowInstance.builder()
                .definitionId(storedDefinition == null ? null : storedDefinition.getId())
                .title(resolveTitle(request))
                .input(request.getInput())
                .status(WorkflowStatus.CREATED.name())
                .currentStep("created")
                .metadata(toJson(new WorkflowMetadata(request.getKnowledgeBaseId(), templateCode)))
                .createdAt(now)
                .updatedAt(now)
                .build();
        workflowInstanceMapper.insert(workflowInstance);

        WorkflowState state = WorkflowState.builder()
                .workflowInstanceId(workflowInstance.getId())
                .title(workflowInstance.getTitle())
                .userInput(request.getInput())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .templateCode(templateCode)
                .traceId(observationRecorder.nextTraceId())
                .retryCount(0)
                .build();
        WorkflowGraphDefinition graph = workflowGraphRegistry.get(templateCode);

        taskExecutor.execute(() -> runWorkflowSafely(workflowInstance, state, graph.firstNode(state), "create"));
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
        state.setRevisionRequest(null);
        state.setApprovalRequired(false);
        updateWorkflow(workflow.getId(), WorkflowStatus.RUNNING.name(), "publish", "Approval passed, continue publishing");
        workflowEventPublisher.workflowResumed(workflow.getId(), "Approval passed, continue with Reporter Agent");
        taskExecutor.execute(() -> runWorkflowSafely(workflow, state, "publish", "approval_resume"));
    }

    public void rejectAndRetry(String approvalRecordId, String comment) {
        ApprovalRecord approval = approvalService.reject(approvalRecordId, comment);
        WorkflowInstance workflow = requireWorkflow(approval.getWorkflowInstanceId());
        WorkflowState state = loadLatestState(workflow);
        state.setApprovalRecordId(approval.getId());
        state.setApprovalStatus(ApprovalStatus.REJECTED.name());
        state.setApprovalComment(approval.getComment());
        state.setRevisionRequest(approval.getComment());
        state.setApprovalRequired(false);
        state.setRetryCount(defaultIfNull(state.getRetryCount(), 0) + 1);
        updateWorkflow(workflow.getId(), WorkflowStatus.RUNNING.name(), "executor", "Approval rejected, return to Executor Agent");
        workflowEventPublisher.workflowResumed(workflow.getId(), "Approval rejected, regenerate from Executor Agent");
        taskExecutor.execute(() -> runWorkflowSafely(workflow, state, "executor", "approval_reject_retry"));
    }

    public GetWorkflowTemplatesResponse getTemplates() {
        return GetWorkflowTemplatesResponse.builder()
                .templates(workflowGraphRegistry.allDefinitions().stream()
                        .map(this::toTemplateVO)
                        .toArray(WorkflowTemplateVO[]::new))
                .build();
    }

    public WorkflowTemplateVO updateTemplate(String templateCode, UpdateWorkflowTemplateRequest request) {
        WorkflowDefinition saved = workflowGraphRegistry.saveTemplate(templateCode, request);
        return toTemplateVO(saved);
    }

    public GetWorkflowTraceResponse getTrace(String workflowInstanceId) {
        requireWorkflow(workflowInstanceId);
        return GetWorkflowTraceResponse.builder()
                .workflowInstanceId(workflowInstanceId)
                .traces(executionTraceRefMapper.selectByWorkflowInstanceId(workflowInstanceId).stream()
                        .map(this::toVO)
                        .toArray(ExecutionTraceRefVO[]::new))
                .build();
    }

    public GetWorkflowObservabilityResponse getObservability(String workflowInstanceId) {
        requireWorkflow(workflowInstanceId);
        return workflowObservabilityService.getObservability(workflowInstanceId);
    }

    public GetWorkflowCheckpointsResponse getCheckpoints(String workflowInstanceId) {
        requireWorkflow(workflowInstanceId);
        return GetWorkflowCheckpointsResponse.builder()
                .workflowInstanceId(workflowInstanceId)
                .checkpoints(workflowExecutionCheckpointMapper.selectByWorkflowInstanceId(workflowInstanceId).stream()
                        .map(this::toVO)
                        .toArray(WorkflowExecutionCheckpointVO[]::new))
                .build();
    }

    public void replayFrom(String workflowInstanceId, String nodeKey) {
        WorkflowInstance workflow = requireWorkflow(workflowInstanceId);
        WorkflowExecutionCheckpoint checkpoint = workflowExecutionCheckpointMapper.selectLatestByWorkflowAndNode(
                workflowInstanceId,
                nodeKey,
                CHECKPOINT_BEFORE_NODE
        );
        WorkflowState state = checkpoint != null
                ? loadStateSnapshot(checkpoint.getStateSnapshot())
                : loadLatestState(workflow);
        state.setRetryCount(defaultIfNull(state.getRetryCount(), 0) + 1);
        updateWorkflow(workflow.getId(), WorkflowStatus.RUNNING.name(), nodeKey, "Replay from node: " + nodeKey);
        workflowEventPublisher.workflowResumed(workflow.getId(), "Replay execution from node: " + nodeKey);
        taskExecutor.execute(() -> runWorkflowSafely(workflow, state, nodeKey, "replay"));
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

    private void runWorkflowSafely(WorkflowInstance workflowInstance,
                                   WorkflowState state,
                                   String startNodeKey,
                                   String runReason) {
        try {
            runGraph(workflowInstance, state, startNodeKey, runReason);
        } catch (Exception e) {
            workflowEventPublisher.workflowFailed(workflowInstance.getId(), resolveErrorMessage(e));
        } finally {
            observationRecorder.clearCurrentThread();
        }
    }

    private void runGraph(WorkflowInstance workflowInstance,
                          WorkflowState state,
                          String startNodeKey,
                          String runReason) {
        WorkflowGraphDefinition graph = workflowGraphRegistry.get(state.getTemplateCode());
        Map<String, WorkflowNode> nodeMap = workflowNodes.stream()
                .collect(Collectors.toMap(WorkflowNode::key, node -> node));
        Map<String, WorkflowGraphNode> graphNodeMap = graph.nodeMap();
        WorkflowMetadata metadata = parseMetadata(workflowInstance.getMetadata());

        String runId = observationRecorder.nextRunId();
        state.setWorkflowInstanceId(workflowInstance.getId());
        state.setTitle(defaultIfNull(state.getTitle(), workflowInstance.getTitle()));
        state.setUserInput(defaultIfNull(state.getUserInput(), workflowInstance.getInput()));
        state.setTemplateCode(defaultIfNull(state.getTemplateCode(), metadata.templateCode()));
        state.setKnowledgeBaseId(defaultIfNull(state.getKnowledgeBaseId(), metadata.knowledgeBaseId()));
        state.setTraceId(observationRecorder.nextTraceId());

        saveCheckpoint(
                workflowInstance.getId(),
                runId,
                state.getTraceId(),
                startNodeKey,
                CHECKPOINT_WORKFLOW_STARTED,
                state,
                attributes("runReason", runReason, "startNodeKey", startNodeKey)
        );

        ObservationScope workflowScope = observationRecorder.startWorkflowRun(
                workflowInstance.getId(),
                state.getTraceId(),
                runId,
                workflowInstance.getTitle(),
                startNodeKey,
                workflowInstance.getInput(),
                attributes(
                        "runReason", runReason,
                        "templateCode", state.getTemplateCode(),
                        "startNodeKey", startNodeKey,
                        "definitionId", workflowInstance.getDefinitionId()
                )
        );

        if ("create".equals(runReason)) {
            workflowEventPublisher.workflowStarted(workflowInstance.getId(), workflowInstance.getTitle());
        }
        updateWorkflow(workflowInstance.getId(), WorkflowStatus.RUNNING.name(), startNodeKey, null);

        String currentNodeKey = startNodeKey;
        try {
            while (!WorkflowGraphDefinition.END.equals(currentNodeKey)) {
                WorkflowNode node = nodeMap.get(currentNodeKey);
                WorkflowGraphNode graphNode = graphNodeMap.get(currentNodeKey);
                if (node == null) {
                    throw new BizException("Graph node implementation not found: " + currentNodeKey);
                }

                state.setCurrentNodeKey(currentNodeKey);
                appendGraphPath(state, currentNodeKey);

                WorkflowStepInstance step = startStep(workflowInstance.getId(), currentNodeKey, node.name(), state);
                workflowEventPublisher.stepStarted(workflowInstance.getId(), step);
                ObservationScope nodeScope = observationRecorder.startNodeRun(
                        workflowInstance.getId(),
                        runId,
                        currentNodeKey,
                        node.name(),
                        step.getInputSnapshot(),
                        attributes("stepId", step.getId(), "nodeKey", currentNodeKey)
                );

                long start = System.currentTimeMillis();
                try {
                    updateWorkflow(workflowInstance.getId(), WorkflowStatus.RUNNING.name(), currentNodeKey, null);
                    saveCheckpoint(
                            workflowInstance.getId(),
                            runId,
                            state.getTraceId(),
                            currentNodeKey,
                            CHECKPOINT_BEFORE_NODE,
                            state,
                            attributes("nodeName", node.name(), "runReason", runReason)
                    );
                    recordTrace(
                            workflowInstance.getId(),
                            state,
                            currentNodeKey,
                            "NODE_STARTED",
                            WorkflowStepStatus.RUNNING.name(),
                            step.getInputSnapshot(),
                            "{}",
                            null,
                            null
                    );

                    WorkflowState outputState = executeGraphNode(
                            workflowInstance.getId(),
                            runId,
                            graph,
                            graphNode,
                            node,
                            state
                    );
                    completeStep(step.getId(), outputState);
                    state = outputState;

                    long durationMs = System.currentTimeMillis() - start;
                    WorkflowStepInstance completedStep = WorkflowStepInstance.builder()
                            .id(step.getId())
                            .workflowInstanceId(step.getWorkflowInstanceId())
                            .nodeKey(step.getNodeKey())
                            .nodeName(step.getNodeName())
                            .status(WorkflowStepStatus.COMPLETED.name())
                            .build();
                    String nodeOutputSummary = summarizeNodeOutput(currentNodeKey, state);
                    workflowEventPublisher.stepCompleted(workflowInstance.getId(), completedStep, nodeOutputSummary);
                    recordTrace(
                            workflowInstance.getId(),
                            state,
                            currentNodeKey,
                            "NODE_COMPLETED",
                            WorkflowStepStatus.COMPLETED.name(),
                            step.getInputSnapshot(),
                            toJson(state),
                            null,
                            durationMs
                    );
                    saveCheckpoint(
                            workflowInstance.getId(),
                            runId,
                            state.getTraceId(),
                            currentNodeKey,
                            CHECKPOINT_AFTER_NODE,
                            state,
                            attributes("nodeName", node.name(), "durationMs", durationMs)
                    );

                    if ("approval".equals(currentNodeKey) && state.isApprovalRequired()) {
                        updateWorkflow(workflowInstance.getId(), WorkflowStatus.WAITING_APPROVAL.name(), "approval", state.getReviewComment());
                        workflowEventPublisher.approvalRequired(
                                workflowInstance.getId(),
                                state.getApprovalRecordId(),
                                state.getReviewComment()
                        );
                        saveCheckpoint(
                                workflowInstance.getId(),
                                runId,
                                state.getTraceId(),
                                currentNodeKey,
                                CHECKPOINT_WAITING_APPROVAL,
                                state,
                                attributes("approvalRecordId", state.getApprovalRecordId())
                        );
                        observationRecorder.complete(
                                nodeScope,
                                ObservationStatus.WAITING_APPROVAL,
                                state.getReviewComment(),
                                attributes("workflowStatus", WorkflowStatus.WAITING_APPROVAL.name())
                        );
                        observationRecorder.complete(
                                workflowScope,
                                ObservationStatus.WAITING_APPROVAL,
                                state.getReviewComment(),
                                attributes(
                                        "approvalRecordId", state.getApprovalRecordId(),
                                        "workflowStatus", WorkflowStatus.WAITING_APPROVAL.name()
                                )
                        );
                        return;
                    }

                    observationRecorder.complete(
                            nodeScope,
                            ObservationStatus.COMPLETED,
                            nodeOutputSummary,
                            attributes("durationMs", durationMs, "stepStatus", WorkflowStepStatus.COMPLETED.name())
                    );
                    currentNodeKey = graph.nextNode(currentNodeKey, state);
                } catch (Exception e) {
                    String errorMessage = resolveErrorMessage(e);
                    failStep(step.getId(), e);
                    updateWorkflow(workflowInstance.getId(), WorkflowStatus.FAILED.name(), currentNodeKey, errorMessage);
                    workflowEventPublisher.stepFailed(workflowInstance.getId(), step, errorMessage);
                    recordTrace(
                            workflowInstance.getId(),
                            state,
                            currentNodeKey,
                            "NODE_FAILED",
                            WorkflowStepStatus.FAILED.name(),
                            step.getInputSnapshot(),
                            "{}",
                            errorMessage,
                            System.currentTimeMillis() - start
                    );
                    saveCheckpoint(
                            workflowInstance.getId(),
                            runId,
                            state.getTraceId(),
                            currentNodeKey,
                            CHECKPOINT_FAILED_NODE,
                            state,
                            attributes("errorMessage", errorMessage)
                    );
                    observationRecorder.fail(
                            nodeScope,
                            e,
                            attributes("stepStatus", WorkflowStepStatus.FAILED.name(), "errorMessage", errorMessage)
                    );
                    throw new BizException("Workflow node execution failed: " + node.name() + ", " + errorMessage);
                }
            }

            updateWorkflow(workflowInstance.getId(), WorkflowStatus.COMPLETED.name(), "finished", state.getFinalOutput());
            saveCheckpoint(
                    workflowInstance.getId(),
                    runId,
                    state.getTraceId(),
                    WORKFLOW_CHECKPOINT_NODE,
                    CHECKPOINT_WORKFLOW_COMPLETED,
                    state,
                    attributes("runReason", runReason)
            );
            workflowEventPublisher.workflowFinished(workflowInstance.getId(), state.getFinalOutput());
            observationRecorder.complete(
                    workflowScope,
                    ObservationStatus.COMPLETED,
                    state.getFinalOutput(),
                    attributes("workflowStatus", WorkflowStatus.COMPLETED.name(), "runReason", runReason)
            );
        } catch (Exception e) {
            observationRecorder.fail(
                    workflowScope,
                    e,
                    attributes(
                            "workflowStatus", WorkflowStatus.FAILED.name(),
                            "currentNodeKey", currentNodeKey,
                            "runReason", runReason
                    )
            );
            throw e;
        }
    }

    private WorkflowState executeGraphNode(String workflowInstanceId,
                                           String runId,
                                           WorkflowGraphDefinition graph,
                                           WorkflowGraphNode graphNode,
                                           WorkflowNode node,
                                           WorkflowState state) {
        if (graphNode != null && graphNode.isSubGraph() && StringUtils.hasText(graphNode.getSubGraphCode())) {
            return executeSubGraph(workflowInstanceId, runId, graph, graphNode, state);
        }
        return node.execute(state);
    }

    private WorkflowState executeSubGraph(String workflowInstanceId,
                                          String runId,
                                          WorkflowGraphDefinition graph,
                                          WorkflowGraphNode graphNode,
                                          WorkflowState state) {
        WorkflowSubGraphDefinition subGraph = graph.subGraph(graphNode.getSubGraphCode());
        if (subGraph == null || subGraph.getGroups() == null || subGraph.getGroups().isEmpty()) {
            return state;
        }

        WorkflowState currentState = state;
        Map<String, Object> subGraphContext = new LinkedHashMap<>();
        ObservationScope parentScope = observationRecorder.currentScope();

        for (WorkflowSubGraphGroup group : subGraph.getGroups()) {
            List<WorkflowSubGraphStep> steps = defaultIfNull(group.getSteps(), List.<WorkflowSubGraphStep>of());
            if (steps.isEmpty()) {
                continue;
            }

            if (group.isParallel()) {
                WorkflowState stateAtGroupStart = cloneState(currentState);
                Map<String, Object> contextAtGroupStart = new LinkedHashMap<>(subGraphContext);
                List<CompletableFuture<SubGraphStepResult>> futures = steps.stream()
                        .map(step -> CompletableFuture.supplyAsync(
                                () -> executeSubGraphStep(
                                        workflowInstanceId,
                                        runId,
                                        graphNode,
                                        group,
                                        step,
                                        cloneState(stateAtGroupStart),
                                        new LinkedHashMap<>(contextAtGroupStart),
                                        parentScope
                                ),
                                taskExecutor
                        ))
                        .toList();

                for (CompletableFuture<SubGraphStepResult> future : futures) {
                    SubGraphStepResult result = joinFuture(future);
                    subGraphContext.putAll(result.contextUpdates());
                    if (result.state() != null) {
                        currentState = result.state();
                    }
                }
            } else {
                for (WorkflowSubGraphStep step : steps) {
                    SubGraphStepResult result = executeSubGraphStep(
                            workflowInstanceId,
                            runId,
                            graphNode,
                            group,
                            step,
                            currentState,
                            subGraphContext,
                            parentScope
                    );
                    subGraphContext.putAll(result.contextUpdates());
                    if (result.state() != null) {
                        currentState = result.state();
                    }
                }
            }
        }

        return currentState;
    }

    private SubGraphStepResult executeSubGraphStep(String workflowInstanceId,
                                                   String runId,
                                                   WorkflowGraphNode parentNode,
                                                   WorkflowSubGraphGroup group,
                                                   WorkflowSubGraphStep subGraphStep,
                                                   WorkflowState state,
                                                   Map<String, Object> subGraphContext,
                                                   ObservationScope parentScope) {
        String stepNodeKey = parentNode.getKey() + "." + subGraphStep.getKey();
        String stepNodeName = parentNode.getName() + " / " + defaultIfNull(subGraphStep.getName(), subGraphStep.getKey());
        WorkflowStepInstance step = startStep(workflowInstanceId, stepNodeKey, stepNodeName, state);
        workflowEventPublisher.stepStarted(workflowInstanceId, step);

        ObservationScope stepScope = observationRecorder.startNodeRun(
                workflowInstanceId,
                runId,
                parentScope,
                stepNodeKey,
                stepNodeName,
                step.getInputSnapshot(),
                attributes(
                        "parentNodeKey", parentNode.getKey(),
                        "subGraphGroup", group.getKey(),
                        "subGraphStep", subGraphStep.getKey(),
                        "handlerType", subGraphStep.getHandlerType()
                )
        );

        long start = System.currentTimeMillis();
        try {
            recordTrace(
                    workflowInstanceId,
                    state,
                    stepNodeKey,
                    "NODE_STARTED",
                    WorkflowStepStatus.RUNNING.name(),
                    step.getInputSnapshot(),
                    "{}",
                    null,
                    null
            );

            SubGraphStepResult result = switch (defaultIfNull(subGraphStep.getHandlerType(), "")) {
                case "RETRIEVAL_BRIEF" -> {
                    String retrievalBrief = workflowSubGraphService.buildRetrievalBrief(cloneState(state));
                    yield new SubGraphStepResult(
                            null,
                            Map.of("retrievalBrief", retrievalBrief),
                            retrievalBrief
                    );
                }
                case "KNOWLEDGE_LOOKUP" -> {
                    List<WorkflowSource> sources = workflowSubGraphService.lookupKnowledge(cloneState(state));
                    yield new SubGraphStepResult(
                            null,
                            Map.of("sources", sources),
                            "Retrieved sources: " + sources.size()
                    );
                }
                case "RETRIEVAL_MERGE" -> {
                    String retrievalBrief = defaultIfNull((String) subGraphContext.get("retrievalBrief"), "");
                    @SuppressWarnings("unchecked")
                    List<WorkflowSource> sources = defaultIfNull((List<WorkflowSource>) subGraphContext.get("sources"), List.of());
                    WorkflowState mergedState = workflowSubGraphService.mergeRetrievalResult(state, retrievalBrief, sources);
                    yield new SubGraphStepResult(
                            mergedState,
                            Map.of(
                                    "retrievalBrief", retrievalBrief,
                                    "sources", sources
                            ),
                            "Merged retrieval context with " + sources.size() + " sources"
                    );
                }
                default -> throw new BizException("Unsupported sub-graph handler: " + subGraphStep.getHandlerType());
            };

            WorkflowState outputState = result.state() != null ? result.state() : state;
            completeStep(step.getId(), outputState);
            long durationMs = System.currentTimeMillis() - start;
            workflowEventPublisher.stepCompleted(
                    workflowInstanceId,
                    WorkflowStepInstance.builder()
                            .id(step.getId())
                            .workflowInstanceId(step.getWorkflowInstanceId())
                            .nodeKey(step.getNodeKey())
                            .nodeName(step.getNodeName())
                            .status(WorkflowStepStatus.COMPLETED.name())
                            .build(),
                    result.outputSummary()
            );
            recordTrace(
                    workflowInstanceId,
                    outputState,
                    stepNodeKey,
                    "NODE_COMPLETED",
                    WorkflowStepStatus.COMPLETED.name(),
                    step.getInputSnapshot(),
                    toJson(outputState),
                    null,
                    durationMs
            );
            saveCheckpoint(
                    workflowInstanceId,
                    runId,
                    outputState.getTraceId(),
                    stepNodeKey,
                    CHECKPOINT_SUBGRAPH_STEP,
                    outputState,
                    attributes(
                            "parentNodeKey", parentNode.getKey(),
                            "subGraphGroup", group.getKey(),
                            "subGraphStep", subGraphStep.getKey(),
                            "handlerType", subGraphStep.getHandlerType(),
                            "durationMs", durationMs
                    )
            );
            observationRecorder.complete(
                    stepScope,
                    ObservationStatus.COMPLETED,
                    result.outputSummary(),
                    attributes(
                            "durationMs", durationMs,
                            "subGraphStep", subGraphStep.getKey(),
                            "subGraphGroup", group.getKey()
                    )
            );
            return result;
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            failStep(step.getId(), e);
            workflowEventPublisher.stepFailed(workflowInstanceId, step, errorMessage);
            recordTrace(
                    workflowInstanceId,
                    state,
                    stepNodeKey,
                    "NODE_FAILED",
                    WorkflowStepStatus.FAILED.name(),
                    step.getInputSnapshot(),
                    "{}",
                    errorMessage,
                    System.currentTimeMillis() - start
            );
            saveCheckpoint(
                    workflowInstanceId,
                    runId,
                    state.getTraceId(),
                    stepNodeKey,
                    CHECKPOINT_FAILED_NODE,
                    state,
                    attributes(
                            "parentNodeKey", parentNode.getKey(),
                            "subGraphStep", subGraphStep.getKey(),
                            "errorMessage", errorMessage
                    )
            );
            observationRecorder.fail(
                    stepScope,
                    e,
                    attributes(
                            "parentNodeKey", parentNode.getKey(),
                            "subGraphStep", subGraphStep.getKey(),
                            "errorMessage", errorMessage
                    )
            );
            throw e;
        }
    }

    private <T> T joinFuture(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BizException(resolveErrorMessage(cause instanceof Exception exception ? exception : e));
        }
    }

    private WorkflowState cloneState(WorkflowState state) {
        return loadStateSnapshot(toJson(state));
    }

    private void saveCheckpoint(String workflowInstanceId,
                                String runId,
                                String traceId,
                                String nodeKey,
                                String checkpointType,
                                WorkflowState state,
                                Object metadata) {
        workflowExecutionCheckpointMapper.insert(WorkflowExecutionCheckpoint.builder()
                .id(UUID.randomUUID().toString())
                .workflowInstanceId(workflowInstanceId)
                .traceId(defaultIfNull(traceId, observationRecorder.nextTraceId()))
                .runId(runId)
                .nodeKey(defaultIfNull(nodeKey, WORKFLOW_CHECKPOINT_NODE))
                .checkpointType(checkpointType)
                .stateSnapshot(toJson(state))
                .metadata(toJson(metadata == null ? Map.of() : metadata))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private WorkflowState loadLatestState(WorkflowInstance workflow) {
        WorkflowExecutionCheckpoint latestCheckpoint = workflowExecutionCheckpointMapper.selectLatestByWorkflowInstanceId(workflow.getId());
        if (latestCheckpoint != null && StringUtils.hasText(latestCheckpoint.getStateSnapshot())) {
            return hydrateLoadedState(loadStateSnapshot(latestCheckpoint.getStateSnapshot()), workflow);
        }

        List<WorkflowStepInstance> steps = workflowStepInstanceMapper.selectByWorkflowInstanceId(workflow.getId());
        for (int index = steps.size() - 1; index >= 0; index--) {
            String snapshot = steps.get(index).getOutputSnapshot();
            if (!StringUtils.hasText(snapshot) || "{}".equals(snapshot)) {
                continue;
            }
            try {
                return hydrateLoadedState(objectMapper.readValue(snapshot, WorkflowState.class), workflow);
            } catch (JsonProcessingException ignored) {
                break;
            }
        }

        WorkflowMetadata metadata = parseMetadata(workflow.getMetadata());
        return WorkflowState.builder()
                .workflowInstanceId(workflow.getId())
                .title(workflow.getTitle())
                .userInput(workflow.getInput())
                .knowledgeBaseId(metadata.knowledgeBaseId())
                .templateCode(defaultIfNull(metadata.templateCode(), workflowGraphRegistry.resolveTemplateCode(null)))
                .traceId(observationRecorder.nextTraceId())
                .retryCount(0)
                .build();
    }

    private WorkflowState loadStateSnapshot(String snapshot) {
        try {
            return objectMapper.readValue(snapshot, WorkflowState.class);
        } catch (JsonProcessingException e) {
            throw new BizException("Workflow state snapshot parse failed: " + e.getMessage());
        }
    }

    private WorkflowMetadata parseMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return new WorkflowMetadata(null, workflowGraphRegistry.resolveTemplateCode(null));
        }
        try {
            return objectMapper.readValue(metadataJson, WorkflowMetadata.class);
        } catch (Exception ignored) {
            return new WorkflowMetadata(null, workflowGraphRegistry.resolveTemplateCode(null));
        }
    }

    private WorkflowState hydrateLoadedState(WorkflowState state, WorkflowInstance workflow) {
        WorkflowMetadata metadata = parseMetadata(workflow.getMetadata());
        state.setWorkflowInstanceId(defaultIfNull(state.getWorkflowInstanceId(), workflow.getId()));
        state.setTitle(defaultIfNull(state.getTitle(), workflow.getTitle()));
        state.setUserInput(defaultIfNull(state.getUserInput(), workflow.getInput()));
        state.setKnowledgeBaseId(defaultIfNull(state.getKnowledgeBaseId(), metadata.knowledgeBaseId()));
        state.setTemplateCode(defaultIfNull(state.getTemplateCode(), metadata.templateCode()));
        state.setTraceId(defaultIfNull(state.getTraceId(), observationRecorder.nextTraceId()));
        return state;
    }

    private void appendGraphPath(WorkflowState state, String nodeKey) {
        if (state.getGraphPath() == null) {
            state.setGraphPath(new ArrayList<>());
        }
        if (state.getGraphPath().isEmpty()
                || !nodeKey.equals(state.getGraphPath().get(state.getGraphPath().size() - 1))) {
            state.getGraphPath().add(nodeKey);
        }
    }

    private void recordTrace(String workflowInstanceId,
                             WorkflowState state,
                             String nodeKey,
                             String eventType,
                             String status,
                             String inputSnapshot,
                             String outputSnapshot,
                             String errorMessage,
                             Long durationMs) {
        ExecutionTraceRef traceRef = ExecutionTraceRef.builder()
                .workflowInstanceId(workflowInstanceId)
                .traceId(StringUtils.hasText(state.getTraceId()) ? state.getTraceId() : observationRecorder.nextTraceId())
                .graphTemplate(StringUtils.hasText(state.getTemplateCode()) ? state.getTemplateCode() : workflowGraphRegistry.resolveTemplateCode(null))
                .nodeKey(nodeKey)
                .eventType(eventType)
                .status(status)
                .inputSnapshot(StringUtils.hasText(inputSnapshot) ? inputSnapshot : "{}")
                .outputSnapshot(StringUtils.hasText(outputSnapshot) ? outputSnapshot : "{}")
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .createdAt(LocalDateTime.now())
                .build();
        executionTraceRefMapper.insert(traceRef);
    }

    private String summarizeNodeOutput(String nodeKey, WorkflowState state) {
        if (nodeKey != null && nodeKey.startsWith("retriever.")) {
            return switch (nodeKey) {
                case "retriever.retrieval_brief" -> defaultIfNull(state.getPlan(), "Retrieval brief generated");
                case "retriever.knowledge_lookup" -> "Retrieved sources: " + state.safeSources().size();
                case "retriever.retrieval_merge" -> String.join("\n", state.safeRetrievedContents());
                default -> state.getFinalOutput();
            };
        }
        return switch (nodeKey) {
            case "planner" -> state.getPlan();
            case "retriever" -> String.join("\n", state.safeRetrievedContents());
            case "executor" -> state.getDraftResult();
            case "reviewer" -> state.getReviewComment();
            case "approval" -> "Waiting for approval: " + state.getApprovalRecordId();
            case "publish" -> state.getFinalOutput();
            default -> state.getFinalOutput();
        };
    }

    private WorkflowStepInstance startStep(String workflowInstanceId, String nodeKey, String nodeName, WorkflowState state) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowStepInstance step = WorkflowStepInstance.builder()
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(nodeKey)
                .nodeName(nodeName)
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
        workflowStepInstanceMapper.updateById(WorkflowStepInstance.builder()
                .id(stepId)
                .status(WorkflowStepStatus.COMPLETED.name())
                .outputSnapshot(toJson(state))
                .completedAt(LocalDateTime.now())
                .build());
    }

    private void failStep(String stepId, Exception e) {
        workflowStepInstanceMapper.updateById(WorkflowStepInstance.builder()
                .id(stepId)
                .status(WorkflowStepStatus.FAILED.name())
                .errorMessage(resolveErrorMessage(e))
                .completedAt(LocalDateTime.now())
                .build());
    }

    private void updateWorkflow(String workflowInstanceId, String status, String currentStep, String result) {
        workflowInstanceMapper.updateById(WorkflowInstance.builder()
                .id(workflowInstanceId)
                .status(status)
                .currentStep(currentStep)
                .result(result)
                .build());
    }

    private WorkflowInstance requireWorkflow(String workflowInstanceId) {
        WorkflowInstance workflow = workflowInstanceMapper.selectById(workflowInstanceId);
        if (workflow == null) {
            throw new BizException("Workflow instance not found: " + workflowInstanceId);
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

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown workflow error";
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("Workflow state serialization failed: " + e.getMessage());
        }
    }

    private Map<String, Object> attributes(Object... keyValues) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        int index = 0;
        while (index + 1 < keyValues.length) {
            Object key = keyValues[index];
            Object value = keyValues[index + 1];
            if (key instanceof String stringKey && value != null) {
                attributes.put(stringKey, value);
            }
            index += 2;
        }
        return attributes;
    }

    private WorkflowTemplateVO toTemplateVO(WorkflowDefinition definition) {
        WorkflowGraphDefinition graphDefinition = workflowGraphRegistry.get(definition.getCode());
        boolean supportsSubGraph = graphDefinition.getNodes().stream().anyMatch(WorkflowGraphNode::isSubGraph)
                || (graphDefinition.getSubGraphs() != null && !graphDefinition.getSubGraphs().isEmpty());
        boolean supportsParallel = graphDefinition.getSubGraphs() != null
                && graphDefinition.getSubGraphs().stream()
                .flatMap(subGraph -> defaultIfNull(subGraph.getGroups(), List.<WorkflowSubGraphGroup>of()).stream())
                .anyMatch(WorkflowSubGraphGroup::isParallel);
        boolean supportsCheckpoint = graphDefinition.getNodes().stream().anyMatch(WorkflowGraphNode::isCheckpointEnabled);

        return WorkflowTemplateVO.builder()
                .code(definition.getCode())
                .name(definition.getName())
                .description(definition.getDescription())
                .mermaid(langGraph4jGraphCompiler.compileMermaid(graphDefinition))
                .definitionJson(definition.getDefinition())
                .sourceType(workflowGraphRegistry.isBuiltInTemplate(definition.getCode()) ? "builtin" : "database")
                .supportsCheckpoint(supportsCheckpoint)
                .supportsSubGraph(supportsSubGraph)
                .supportsParallel(supportsParallel)
                .build();
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

    private ExecutionTraceRefVO toVO(ExecutionTraceRef traceRef) {
        return ExecutionTraceRefVO.builder()
                .id(traceRef.getId())
                .workflowInstanceId(traceRef.getWorkflowInstanceId())
                .traceId(traceRef.getTraceId())
                .graphTemplate(traceRef.getGraphTemplate())
                .nodeKey(traceRef.getNodeKey())
                .eventType(traceRef.getEventType())
                .status(traceRef.getStatus())
                .inputSnapshot(traceRef.getInputSnapshot())
                .outputSnapshot(traceRef.getOutputSnapshot())
                .errorMessage(traceRef.getErrorMessage())
                .durationMs(traceRef.getDurationMs())
                .createdAt(traceRef.getCreatedAt())
                .build();
    }

    private WorkflowExecutionCheckpointVO toVO(WorkflowExecutionCheckpoint checkpoint) {
        return WorkflowExecutionCheckpointVO.builder()
                .id(checkpoint.getId())
                .workflowInstanceId(checkpoint.getWorkflowInstanceId())
                .traceId(checkpoint.getTraceId())
                .runId(checkpoint.getRunId())
                .nodeKey(checkpoint.getNodeKey())
                .checkpointType(checkpoint.getCheckpointType())
                .stateSnapshot(checkpoint.getStateSnapshot())
                .metadata(checkpoint.getMetadata())
                .createdAt(checkpoint.getCreatedAt())
                .build();
    }

    private <T> T defaultIfNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private record WorkflowMetadata(String knowledgeBaseId, String templateCode) {
    }

    private record SubGraphStepResult(WorkflowState state,
                                      Map<String, Object> contextUpdates,
                                      String outputSummary) {
    }
}
