package com.kama.jchatmind.agent.tools.test;

import com.kama.jchatmind.agent.tools.Tool;
import com.kama.jchatmind.agent.tools.ToolType;
import org.springframework.stereotype.Component;

@Component
public class CityTool implements Tool {

    @Override
    public String getName() {
        return "getCity";
    }

    @Override
    public String getDescription() {
        return "获取当前城市。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @dev.langchain4j.agent.tool.Tool(name = "getCity", value = "获取当前所在城市。")
    public String getCity() {
        return "上海";
    }
}
