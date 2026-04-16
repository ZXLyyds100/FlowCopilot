package aliang.flowcopilot.service.impl;

import aliang.flowcopilot.agent.tools.Tool;
import aliang.flowcopilot.agent.tools.ToolType;
import aliang.flowcopilot.service.ToolFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工具门面服务实现。
 * <p>
 * 通过 Spring 自动注入的工具集合来构建系统可用工具，并按类型提供筛选结果。
 */
@Service
@AllArgsConstructor
public class ToolFacadeServiceImpl implements ToolFacadeService {

    private final List<Tool> tools;

    @Override
    /**
     * 返回系统中注册的全部工具。
     *
     * @return 工具列表
     */
    public List<Tool> getAllTools() {
        return tools;
    }

    @Override
    /**
     * 返回全部可选工具。
     *
     * @return 可选工具列表
     */
    public List<Tool> getOptionalTools() {
        return getToolsByType(ToolType.OPTIONAL);
    }

    @Override
    /**
     * 返回全部固定工具。
     *
     * @return 固定工具列表
     */
    public List<Tool> getFixedTools() {
        return getToolsByType(ToolType.FIXED);
    }

    /**
     * 按工具类型筛选工具。
     *
     * @param type 工具类型
     * @return 指定类型工具列表
     */
    private List<Tool> getToolsByType(ToolType type) {
        return tools.stream()
                .filter(tool -> tool.getType().equals(type))
                .toList();
    }
}
