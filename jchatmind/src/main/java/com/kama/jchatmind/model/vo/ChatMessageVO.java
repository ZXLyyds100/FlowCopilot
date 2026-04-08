package com.kama.jchatmind.model.vo;

import com.kama.jchatmind.model.dto.ChatMessageDTO;
import lombok.Builder;
import lombok.Data;

/**
 * 聊天消息前端展示对象。
 */
@Data
@Builder
public class ChatMessageVO {
    private String id;
    private String sessionId;
    private ChatMessageDTO.RoleType role;
    private String content;
    private ChatMessageDTO.MetaData metadata;
}
