package com.kama.jchatmind.agent.tools;

// 暂不注册为 Spring Bean
public class DirectAnswerTool implements Tool {

    @Override
    public String getName() {
        return "directAnswer";
    }

    @Override
    public String getDescription() {
        return "用于直接给出最终回答。";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @dev.langchain4j.agent.tool.Tool(
            name = "directAnswer",
            value = "当用户请求不需要额外工具或计划时，调用此工具表示可以直接输出最终回答。"
    )
    public void directAnswer() {
    }
}
