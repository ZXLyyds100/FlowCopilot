package aliang.flowcopilot.workflow.graph;

/**
 * Built-in graph templates supported by the fourth-stage platform runtime.
 */
public enum WorkflowTemplateType {
    RESEARCH("research", "ResearchWorkflow", "研究型知识增强流程"),
    ANALYSIS("analysis", "AnalysisWorkflow", "分析型流程"),
    TASK_EXECUTION("task_execution", "TaskExecutionWorkflow", "任务执行流程");

    private final String code;
    private final String name;
    private final String description;

    WorkflowTemplateType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return name;
    }

    public String description() {
        return description;
    }

    public static WorkflowTemplateType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RESEARCH;
        }
        for (WorkflowTemplateType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return RESEARCH;
    }
}
