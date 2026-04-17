package aliang.flowcopilot.controller;

import aliang.flowcopilot.agent.tools.Tool;
import aliang.flowcopilot.model.common.ApiResponse;
import aliang.flowcopilot.service.ToolFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具控制器。
 * <p>
 * 给前端返回系统中可配置的工具列表，便于用户在 Agent 配置页中选择启用哪些工具。
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ToolController {

    private final ToolFacadeService toolFacadeService;

    /**
     * 查询当前系统中全部可选工具。
     *
     * @return 可选工具列表
     */
    @GetMapping("/tools")
    public ApiResponse<List<Tool>> getOptionalTools() {
        return ApiResponse.success(toolFacadeService.getOptionalTools());
    }
}
