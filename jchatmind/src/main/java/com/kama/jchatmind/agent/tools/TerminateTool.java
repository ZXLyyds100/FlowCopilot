package com.kama.jchatmind.agent.tools;

import org.springframework.stereotype.Component;

@Component
public class TerminateTool implements Tool {

    @Override
    public String getName() {
        return "terminate";
    }

    @Override
    public String getDescription() {
        return "跳出 Agent Loop 的工具。";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @dev.langchain4j.agent.tool.Tool(name = "terminate", value = "当任务已经执行完毕时调用此工具以结束当前 Agent 循环。")
    public void terminate() {
    }
}
