package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent 表访问接口。
 * <p>
 * 负责 Agent 配置数据的新增、查询、更新和删除。
 */
@Mapper
public interface AgentMapper {
    /**
     * 插入一条 Agent 记录。
     *
     * @param agent Agent 实体
     * @return 受影响行数
     */
    int insert(Agent agent);

    /**
     * 按主键查询 Agent。
     *
     * @param id Agent 主键
     * @return Agent 实体；不存在时返回 {@code null}
     */
    Agent selectById(String id);

    /**
     * 查询全部 Agent。
     *
     * @return Agent 列表
     */
    List<Agent> selectAll();

    /**
     * 按主键删除 Agent。
     *
     * @param id Agent 主键
     * @return 受影响行数
     */
    int deleteById(String id);

    /**
     * 更新 Agent 配置。
     *
     * @param agent 需要更新的 Agent 实体
     * @return 受影响行数
     */
    int updateById(Agent agent);
}
