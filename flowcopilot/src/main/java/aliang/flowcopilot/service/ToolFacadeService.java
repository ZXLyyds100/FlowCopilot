package aliang.flowcopilot.service;

import aliang.flowcopilot.agent.tools.Tool;

import java.util.List;

/**
 * 工具门面服务。
 * <p>
 * 用于从 Spring 容器收集全部工具，并按固定工具与可选工具进行分类输出。
 */
public interface ToolFacadeService {
    /**
     * 获取系统中注册的全部工具。
     *
     * @return 工具列表
     */
    List<Tool> getAllTools();

    /**
     * 获取可由 Agent 配置开关控制的可选工具。
     *
     * @return 可选工具列表
     */
    List<Tool> getOptionalTools();

    /**
     * 获取所有 Agent 默认具备的固定工具。
     *
     * @return 固定工具列表
     */
    List<Tool> getFixedTools();
}
