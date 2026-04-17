package aliang.flowcopilot.workflow.rag;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Small knowledge index boundary for the second-stage workflow.
 */
@Service
public class KnowledgeIndexService {

    public boolean hasKnowledgeScope(String knowledgeBaseId) {
        return StringUtils.hasText(knowledgeBaseId);
    }
}
