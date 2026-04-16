package aliang.flowcopilot.workflow.graph;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;

/**
 * Minimal AgentState adapter used to compile FlowCopilot graph templates with LangGraph4j.
 */
public class LangGraph4jWorkflowState extends AgentState {

    public LangGraph4jWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<String> route(String source) {
        return this.value("route." + source);
    }
}
