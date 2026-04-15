package com.kama.jchatmind.agent.tools.test;

import com.kama.jchatmind.agent.tools.Tool;
import com.kama.jchatmind.agent.tools.ToolType;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool implements Tool {

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return "获取天气。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @dev.langchain4j.agent.tool.Tool(name = "weather", value = "根据城市查询天气。")
    public String weather(String city) {
        return city + "：晴，25°C";
    }
}
