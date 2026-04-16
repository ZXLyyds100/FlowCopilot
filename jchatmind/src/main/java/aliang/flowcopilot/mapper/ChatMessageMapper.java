package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 聊天消息表访问接口。
 * <p>
 * 负责消息的新增、查询、更新和删除，并支持按会话查询最近消息。
 */
@Mapper
public interface ChatMessageMapper {
    /**
     * 插入消息记录。
     *
     * @param chatMessage 消息实体
     * @return 受影响行数
     */
    int insert(ChatMessage chatMessage);

    /**
     * 按主键查询消息。
     *
     * @param id 消息主键
     * @return 消息实体
     */
    ChatMessage selectById(String id);

    /**
     * 查询指定会话的全部消息。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> selectBySessionId(String sessionId);

    /**
     * 查询指定会话最近的若干条消息。
     *
     * @param sessionId 会话 ID
     * @param limit 最大返回数量
     * @return 最近消息列表
     */
    List<ChatMessage> selectBySessionIdRecently(String sessionId, int limit);

    /**
     * 删除消息。
     *
     * @param id 消息主键
     * @return 受影响行数
     */
    int deleteById(String id);

    /**
     * 更新消息。
     *
     * @param chatMessage 消息实体
     * @return 受影响行数
     */
    int updateById(ChatMessage chatMessage);
}
