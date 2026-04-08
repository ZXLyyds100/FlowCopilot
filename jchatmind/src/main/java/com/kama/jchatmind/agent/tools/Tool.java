package com.kama.jchatmind.agent.tools;

/**
 * Agent 工具统一抽象。
 * <p>
 * 所有可以被 Agent 调用的能力都需要实现该接口，并声明名称、说明以及工具类型。
 */
public interface Tool {
    /**
     * 返回工具在运行时的唯一名称。
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 返回工具的人类可读描述，供模型理解工具用途。
     *
     * @return 工具说明
     */
    String getDescription();

    /**
     * 返回工具所属的类型。
     *
     * @return 固定工具或可选工具
     */
    ToolType getType();
}
