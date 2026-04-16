package com.kama.jchatmind.agent.tools.test;

import com.kama.jchatmind.agent.tools.Tool;
import com.kama.jchatmind.agent.tools.ToolType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DateTool implements Tool {

    @Override
    public String getName() {
        return "getDate";
    }

    @Override
    public String getDescription() {
        return "获取当前日期。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @dev.langchain4j.agent.tool.Tool(name = "getDate", value = "获取当前日期。")
    public String getDate() {
        return LocalDate.now().toString();
    }
}
