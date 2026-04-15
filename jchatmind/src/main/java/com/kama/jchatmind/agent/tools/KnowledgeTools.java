package com.kama.jchatmind.agent.tools;

import com.kama.jchatmind.service.RagService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeTools implements Tool {

    private final RagService ragService;

    public KnowledgeTools(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String getName() {
        return "KnowledgeTool";
    }

    @Override
    public String getDescription() {
        return "用于从知识库中执行语义检索。";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @dev.langchain4j.agent.tool.Tool(
            name = "KnowledgeTool",
            value = "从指定知识库中执行相似度检索。参数为知识库 ID 和查询文本，返回最相关的知识片段。"
    )
    public String knowledgeQuery(String kbsId, String query) {
        List<String> strings = ragService.similaritySearch(kbsId, query);
        return String.join("\n", strings);
    }
}
