package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.mapper.ArtifactMapper;
import aliang.flowcopilot.model.entity.Artifact;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publishes the workflow result as an artifact.
 */
@Component
@AllArgsConstructor
public class PublishNode implements WorkflowNode {

    private final ArtifactMapper artifactMapper;

    @Override
    public String key() {
        return "publish";
    }

    @Override
    public String name() {
        return "Publish";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        String finalOutput = """
                # %s

                %s
                """.formatted(state.getTitle(), state.getDraftResult()).strip();
        LocalDateTime now = LocalDateTime.now();
        Artifact artifact = Artifact.builder()
                .workflowInstanceId(state.getWorkflowInstanceId())
                .type("markdown_report")
                .title(state.getTitle())
                .content(finalOutput)
                .metadata("{}")
                .createdAt(now)
                .updatedAt(now)
                .build();
        artifactMapper.insert(artifact);
        state.setArtifactId(artifact.getId());
        state.setFinalOutput(finalOutput);
        return state;
    }
}
