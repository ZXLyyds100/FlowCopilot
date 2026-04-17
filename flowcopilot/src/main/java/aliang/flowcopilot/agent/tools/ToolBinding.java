package aliang.flowcopilot.agent.tools;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ToolBinding {

    private final String name;
    private final ToolSpecification specification;
    private final ToolExecutor executor;
}
