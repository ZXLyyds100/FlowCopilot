package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.service.RagService;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Retrieves optional knowledge context for the workflow.
 */
@Component
@AllArgsConstructor
public class RetrieverNode implements WorkflowNode {

    private final RagService ragService;

    @Override
    public String key() {
        return "retriever";
    }

    @Override
    public String name() {
        return "Retriever";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        if (!StringUtils.hasText(state.getKnowledgeBaseId())) {
            state.setRetrievedContents(List.of("未指定知识库，本轮使用用户输入和通用流程模板执行。"));
            return state;
        }
        try {
            List<String> results = ragService.similaritySearch(state.getKnowledgeBaseId(), state.getUserInput());
            state.setRetrievedContents(results.isEmpty()
                    ? List.of("知识库未召回相关内容。")
                    : results);
        } catch (Exception e) {
            state.setRetrievedContents(List.of("知识检索暂不可用：" + e.getMessage()));
        }
        return state;
    }
}
