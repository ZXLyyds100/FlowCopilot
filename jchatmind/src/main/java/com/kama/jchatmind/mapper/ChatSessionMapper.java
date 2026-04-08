package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 聊天会话表访问接口。
 * <p>
 * 负责会话的持久化与查询，支持按 Agent 维度筛选会话。
 */
@Mapper
public interface ChatSessionMapper {
    /**
     * 插入会话。
     *
     * @param chatSession 会话实体
     * @return 受影响行数
     */
    int insert(ChatSession chatSession);

    /**
     * 按主键查询会话。
     *
     * @param id 会话主键
     * @return 会话实体
     */
    ChatSession selectById(String id);

    /**
     * 查询全部会话。
     *
     * @return 会话列表
     */
    List<ChatSession> selectAll();

    /**
     * 查询指定 Agent 关联的全部会话。
     *
     * @param agentId Agent ID
     * @return 会话列表
     */
    List<ChatSession> selectByAgentId(String agentId);

    /**
     * 删除会话。
     *
     * @param id 会话主键
     * @return 受影响行数
     */
    int deleteById(String id);

    /**
     * 更新会话。
     *
     * @param chatSession 会话实体
     * @return 受影响行数
     */
    int updateById(ChatSession chatSession);
}
