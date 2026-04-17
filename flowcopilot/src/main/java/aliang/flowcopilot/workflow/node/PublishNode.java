package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.ArtifactMapper;
import aliang.flowcopilot.model.entity.Artifact;
import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.state.WorkflowReview;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import aliang.flowcopilot.workflow.state.WorkflowState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Publishes the workflow result as an artifact.
 */
@Component
@AllArgsConstructor
public class PublishNode implements WorkflowNode {

    private final ArtifactMapper artifactMapper;
    private final ObjectMapper objectMapper;
    private final AgentRoleService agentRoleService;

    @Override
    public String key() {
        return "publish";
    }

    @Override
    public String name() {
        return "Reporter Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.REPORTER);
        String finalOutput = """
                # %s

                > 由 %s 整理发布。

                %s

                ---

                ## Reviewer 复核意见
                %s

                ## 知识引用
                %s
                """.formatted(
                state.getTitle(),
                profile.getDisplayName(),
                state.getDraftResult(),
                formatReview(state.getReview()),
                formatSources(state.safeSources())
        ).strip();
        LocalDateTime now = LocalDateTime.now();
        Artifact artifact = Artifact.builder()
                .workflowInstanceId(state.getWorkflowInstanceId())
                .type("markdown_report")
                .title(state.getTitle())
                .content(finalOutput)
                .metadata(toJson(new ArtifactMetadata(profile.getDisplayName(), state.getTaskType(), state.getReview(), state.safeSources())))
                .createdAt(now)
                .updatedAt(now)
                .build();
        artifactMapper.insert(artifact);
        state.setArtifactId(artifact.getId());
        state.setFinalOutput(finalOutput);
        return state;
    }

    private String formatReview(WorkflowReview review) {
        if (review == null) {
            return "Reviewer Agent 暂无复核意见。";
        }
        return """
                - 评分：%d/100
                - 结论：%s
                - 意见：%s
                - 建议：%s
                """.formatted(
                review.getScore(),
                review.isPassed() ? "通过" : "需要修改",
                review.getComment(),
                String.join("；", review.getSuggestions())
        ).strip();
    }

    private String formatSources(List<WorkflowSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return "- 暂无引用来源";
        }
        return String.join("\n", sources.stream()
                .map(source -> "- [%d] %s：%s".formatted(source.getIndex(), source.getTitle(), source.getContent()))
                .toList());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("产物元数据序列化失败: " + e.getMessage());
        }
    }

    private record ArtifactMetadata(String reporter, String taskType, WorkflowReview review, List<WorkflowSource> sources) {
    }
}
