package aliang.flowcopilot.workflow.graph;

import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.WorkflowDefinitionMapper;
import aliang.flowcopilot.model.entity.WorkflowDefinition;
import aliang.flowcopilot.model.request.UpdateWorkflowTemplateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of persisted fourth-stage graph templates.
 */
@Service
@AllArgsConstructor
public class WorkflowGraphRegistry {

    private static final String DEFAULT_TEMPLATE = WorkflowTemplateType.RESEARCH.code();

    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void initialize() {
        builtInDefinitions().values().forEach(this::ensureTemplateExists);
    }

    public WorkflowGraphDefinition get(String templateCode) {
        String resolvedCode = resolveTemplateCode(templateCode);
        WorkflowDefinition stored = workflowDefinitionMapper.selectByCode(resolvedCode);
        if (stored == null) {
            return builtInDefinitions().get(DEFAULT_TEMPLATE);
        }
        return parseDefinition(stored.getDefinition(), stored.getCode(), stored.getName(), stored.getDescription());
    }

    public WorkflowDefinition getStoredDefinition(String templateCode) {
        String resolvedCode = resolveTemplateCode(templateCode);
        WorkflowDefinition stored = workflowDefinitionMapper.selectByCode(resolvedCode);
        if (stored != null) {
            return stored;
        }
        WorkflowGraphDefinition builtIn = builtInDefinitions().get(resolvedCode);
        ensureTemplateExists(builtIn);
        return workflowDefinitionMapper.selectByCode(resolvedCode);
    }

    public List<WorkflowDefinition> allDefinitions() {
        initializeMissingBuiltIns();
        return workflowDefinitionMapper.selectAll();
    }

    public List<WorkflowGraphDefinition> all() {
        initializeMissingBuiltIns();
        return workflowDefinitionMapper.selectAll().stream()
                .map(record -> parseDefinition(record.getDefinition(), record.getCode(), record.getName(), record.getDescription()))
                .toList();
    }

    public String resolveTemplateCode(String templateCode) {
        if (templateCode != null && !templateCode.isBlank() && workflowDefinitionMapper.selectByCode(templateCode) != null) {
            return templateCode;
        }
        if (templateCode != null && !templateCode.isBlank() && builtInDefinitions().containsKey(templateCode)) {
            return templateCode;
        }
        return DEFAULT_TEMPLATE;
    }

    public boolean isBuiltInTemplate(String templateCode) {
        return templateCode != null && builtInDefinitions().containsKey(templateCode);
    }

    public WorkflowDefinition saveTemplate(String templateCode, UpdateWorkflowTemplateRequest request) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new BizException("Template code must not be blank");
        }
        WorkflowDefinition existing = workflowDefinitionMapper.selectByCode(templateCode);
        WorkflowGraphDefinition definition = request.getDefinitionJson() == null || request.getDefinitionJson().isBlank()
                ? (existing == null
                ? builtInDefinitions().get(resolveTemplateCode(templateCode))
                : parseDefinition(existing.getDefinition(), existing.getCode(), existing.getName(), existing.getDescription()))
                : parseDefinition(request.getDefinitionJson(), templateCode, request.getName(), request.getDescription());

        definition.setCode(templateCode);
        if (request.getName() != null && !request.getName().isBlank()) {
            definition.setName(request.getName());
        } else if (definition.getName() == null || definition.getName().isBlank()) {
            definition.setName(templateCode);
        }
        if (request.getDescription() != null) {
            definition.setDescription(request.getDescription());
        }

        WorkflowDefinition record = WorkflowDefinition.builder()
                .code(templateCode)
                .name(definition.getName())
                .description(definition.getDescription())
                .definition(toJson(definition))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (existing == null) {
            workflowDefinitionMapper.insert(record);
        } else {
            workflowDefinitionMapper.updateByCode(record);
        }
        return workflowDefinitionMapper.selectByCode(templateCode);
    }

    private void initializeMissingBuiltIns() {
        builtInDefinitions().values().forEach(this::ensureTemplateExists);
    }

    private void ensureTemplateExists(WorkflowGraphDefinition definition) {
        if (definition == null || workflowDefinitionMapper.selectByCode(definition.getCode()) != null) {
            return;
        }
        workflowDefinitionMapper.insert(WorkflowDefinition.builder()
                .code(definition.getCode())
                .name(definition.getName())
                .description(definition.getDescription())
                .definition(toJson(definition))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private WorkflowGraphDefinition parseDefinition(String definitionJson,
                                                    String fallbackCode,
                                                    String fallbackName,
                                                    String fallbackDescription) {
        try {
            WorkflowGraphDefinition definition = objectMapper.readValue(definitionJson, WorkflowGraphDefinition.class);
            if (definition.getCode() == null || definition.getCode().isBlank()) {
                definition.setCode(fallbackCode);
            }
            if (definition.getName() == null || definition.getName().isBlank()) {
                definition.setName(fallbackName);
            }
            if (definition.getDescription() == null || definition.getDescription().isBlank()) {
                definition.setDescription(fallbackDescription);
            }
            return definition;
        } catch (Exception e) {
            throw new BizException("Workflow template definition parse failed: " + e.getMessage());
        }
    }

    private String toJson(WorkflowGraphDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            throw new BizException("Workflow template serialization failed: " + e.getMessage());
        }
    }

    private Map<String, WorkflowGraphDefinition> builtInDefinitions() {
        WorkflowSubGraphDefinition retrievalSubGraph = WorkflowSubGraphDefinition.builder()
                .code("retrieval_context_subgraph")
                .name("知识检索子图")
                .description("先并行准备检索摘要和知识召回，再合并成 Retriever 节点上下文。")
                .groups(List.of(
                        WorkflowSubGraphGroup.builder()
                                .key("parallel_prepare")
                                .name("并行检索准备")
                                .parallel(true)
                                .steps(List.of(
                                        WorkflowSubGraphStep.builder()
                                                .key("retrieval_brief")
                                                .name("检索摘要")
                                                .handlerType("RETRIEVAL_BRIEF")
                                                .build(),
                                        WorkflowSubGraphStep.builder()
                                                .key("knowledge_lookup")
                                                .name("知识召回")
                                                .handlerType("KNOWLEDGE_LOOKUP")
                                                .build()
                                ))
                                .build(),
                        WorkflowSubGraphGroup.builder()
                                .key("merge_results")
                                .name("合并检索结果")
                                .parallel(false)
                                .steps(List.of(
                                        WorkflowSubGraphStep.builder()
                                                .key("retrieval_merge")
                                                .name("检索结果合并")
                                                .handlerType("RETRIEVAL_MERGE")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        List<WorkflowGraphNode> commonNodes = List.of(
                node("planner", "Planner Agent", "planner", false, null),
                node("retriever", "Retriever Agent", "retriever", true, retrievalSubGraph.getCode()),
                node("executor", "Executor Agent", "executor", false, null),
                node("reviewer", "Reviewer Agent", "reviewer", false, null),
                node("approval", "Human Approval", "human", false, null),
                node("publish", "Reporter Agent", "reporter", false, null)
        );

        WorkflowGraphDefinition research = WorkflowGraphDefinition.builder()
                .code(WorkflowTemplateType.RESEARCH.code())
                .name(WorkflowTemplateType.RESEARCH.displayName())
                .description("研究型工作流，包含检索子图、审批和节点重放。")
                .nodes(commonNodes)
                .edges(List.of(
                        edge(WorkflowGraphDefinition.START, "planner", "start"),
                        edge("planner", "retriever", "need knowledge"),
                        edge("retriever", "executor", "context ready"),
                        edge("executor", "reviewer", "draft"),
                        conditional("reviewer", "executor", "review retry", "review_retry"),
                        conditional("reviewer", "approval", "review pass", "review_pass"),
                        edge("approval", "publish", "approved"),
                        edge("publish", WorkflowGraphDefinition.END, "done")
                ))
                .subGraphs(List.of(retrievalSubGraph))
                .build();

        WorkflowGraphDefinition analysis = WorkflowGraphDefinition.builder()
                .code(WorkflowTemplateType.ANALYSIS.code())
                .name(WorkflowTemplateType.ANALYSIS.displayName())
                .description("分析型工作流，支持检索子图和自动发布分支。")
                .nodes(commonNodes)
                .edges(List.of(
                        edge(WorkflowGraphDefinition.START, "planner", "start"),
                        edge("planner", "retriever", "collect evidence"),
                        edge("retriever", "executor", "analyze"),
                        edge("executor", "reviewer", "review"),
                        conditional("reviewer", "executor", "revise", "review_retry"),
                        conditional("reviewer", "publish", "auto publish", "review_pass"),
                        edge("publish", WorkflowGraphDefinition.END, "done")
                ))
                .subGraphs(List.of(retrievalSubGraph))
                .build();

        WorkflowGraphDefinition taskExecution = WorkflowGraphDefinition.builder()
                .code(WorkflowTemplateType.TASK_EXECUTION.code())
                .name(WorkflowTemplateType.TASK_EXECUTION.displayName())
                .description("任务执行型工作流，存在知识库时进入并行检索子图。")
                .nodes(commonNodes)
                .edges(List.of(
                        edge(WorkflowGraphDefinition.START, "planner", "start"),
                        conditional("planner", "retriever", "has kb", "has_kb"),
                        conditional("planner", "executor", "no kb", "no_kb"),
                        edge("retriever", "executor", "context"),
                        edge("executor", "reviewer", "review"),
                        conditional("reviewer", "executor", "retry", "review_retry"),
                        conditional("reviewer", "approval", "approval", "review_pass"),
                        edge("approval", "publish", "approved"),
                        edge("publish", WorkflowGraphDefinition.END, "done")
                ))
                .subGraphs(List.of(retrievalSubGraph))
                .build();

        Map<String, WorkflowGraphDefinition> definitions = new LinkedHashMap<>();
        definitions.put(research.getCode(), research);
        definitions.put(analysis.getCode(), analysis);
        definitions.put(taskExecution.getCode(), taskExecution);
        return definitions;
    }

    private WorkflowGraphNode node(String key, String name, String role, boolean subGraph, String subGraphCode) {
        return WorkflowGraphNode.builder()
                .key(key)
                .name(name)
                .role(role)
                .subGraph(subGraph)
                .subGraphCode(subGraphCode)
                .checkpointEnabled(true)
                .build();
    }

    private WorkflowGraphEdge edge(String source, String target, String label) {
        return WorkflowGraphEdge.builder()
                .source(source)
                .target(target)
                .label(label)
                .conditional(false)
                .build();
    }

    private WorkflowGraphEdge conditional(String source, String target, String label, String conditionKey) {
        return WorkflowGraphEdge.builder()
                .source(source)
                .target(target)
                .label(label)
                .conditional(true)
                .conditionKey(conditionKey)
                .build();
    }
}
