package aliang.flowcopilot.workflow.rag;

import aliang.flowcopilot.workflow.state.WorkflowSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Converts raw RAG hits into workflow-visible source records.
 */
@Service
public class RetrievalRecordService {

    public List<WorkflowSource> fromKnowledgeHits(List<String> hits) {
        return IntStream.range(0, hits.size())
                .mapToObj(index -> WorkflowSource.builder()
                        .index(index + 1)
                        .sourceType("knowledge_base")
                        .title("知识片段 " + (index + 1))
                        .content(hits.get(index))
                        .metadata("{}")
                        .build())
                .toList();
    }

    public WorkflowSource fallbackSource(String content) {
        return WorkflowSource.builder()
                .index(1)
                .sourceType("workflow_context")
                .title("通用流程上下文")
                .content(content)
                .metadata("{}")
                .build();
    }
}
