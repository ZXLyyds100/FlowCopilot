package com.kama.jchatmind.controller;

import com.kama.jchatmind.model.common.ApiResponse;
import com.kama.jchatmind.model.request.CreateChatMessageRequest;
import com.kama.jchatmind.model.request.UpdateChatMessageRequest;
import com.kama.jchatmind.model.response.CreateChatMessageResponse;
import com.kama.jchatmind.model.response.GetChatMessagesResponse;
import com.kama.jchatmind.service.ChatMessageFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天消息控制器。
 * <p>
 * 用户发消息、查询历史消息、删除消息和更新消息都会经过这一层。
 * 其中创建消息是聊天主链路的入口之一。
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ChatMessageController {

    private final ChatMessageFacadeService chatMessageFacadeService;

    /**
     * 按会话查询聊天消息列表。
     *
     * @param sessionId 会话 ID
     * @return 指定会话的历史消息
     */
    @GetMapping("/chat-messages/session/{sessionId}")
    public ApiResponse<GetChatMessagesResponse> getChatMessagesBySessionId(@PathVariable String sessionId) {
        return ApiResponse.success(chatMessageFacadeService.getChatMessagesBySessionId(sessionId));
    }

    /**
     * 创建一条聊天消息。
     * <p>
     * 当角色为用户消息时，业务层会继续发布聊天事件并驱动 Agent 执行。
     *
     * @param request 创建消息请求
     * @return 新建消息 ID
     */
    @PostMapping("/chat-messages")
    public ApiResponse<CreateChatMessageResponse> createChatMessage(@RequestBody CreateChatMessageRequest request) {
        return ApiResponse.success(chatMessageFacadeService.createChatMessage(request));
    }

    /**
     * 删除指定聊天消息。
     *
     * @param chatMessageId 消息 ID
     * @return 空成功响应
     */
    @DeleteMapping("/chat-messages/{chatMessageId}")
    public ApiResponse<Void> deleteChatMessage(@PathVariable String chatMessageId) {
        chatMessageFacadeService.deleteChatMessage(chatMessageId);
        return ApiResponse.success();
    }

    /**
     * 更新指定聊天消息。
     *
     * @param chatMessageId 消息 ID
     * @param request 更新请求
     * @return 空成功响应
     */
    @PatchMapping("/chat-messages/{chatMessageId}")
    public ApiResponse<Void> updateChatMessage(@PathVariable String chatMessageId, @RequestBody UpdateChatMessageRequest request) {
        chatMessageFacadeService.updateChatMessage(chatMessageId, request);
        return ApiResponse.success();
    }
}
