package aliang.flowcopilot.workflow.ai;

import aliang.flowcopilot.workflow.agent.ToolRegistry;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates structured node output with LangChain4j and falls back to deterministic templates.
 */
@Slf4j
@Service
@AllArgsConstructor
public class StructuredOutputService {

    private final ChatModelProvider chatModelProvider;
    private final ToolRegistry toolRegistry;

    public String generateOrFallback(WorkflowAgentProfile profile, String taskPrompt, String fallback) {
        try {
            String prompt = """
                    【角色职责】
                    %s

                    【允许工具边界】
                    %s

                    【输出要求】
                    请使用 Markdown，至少包含：
                    - 目标理解
                    - 关键依据
                    - 结构化结果

                    【任务】
                    %s
                    """.formatted(
                    profile.getResponsibility(),
                    toolRegistry.allowedTools(profile.getRole()),
                    taskPrompt
            );
            String result = chatModelProvider.chat(profile.getSystemPrompt(), prompt);
            return result == null || result.isBlank() ? fallback : result.strip();
        } catch (Exception e) {
            log.warn("{} fell back to deterministic output: {}", profile.getDisplayName(), e.getMessage());
            return fallback;
        }
    }
}
