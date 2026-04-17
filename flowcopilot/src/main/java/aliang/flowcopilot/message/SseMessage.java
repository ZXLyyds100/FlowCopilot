package aliang.flowcopilot.message;

import aliang.flowcopilot.model.vo.ChatMessageVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * SSE 业务消息对象。
 * <p>
 * 用于后端向前端推送 Agent 状态、生成内容和附加元数据。
 */
@Data
@AllArgsConstructor
@Builder
public class SseMessage {

    private Type type;
    private Payload payload;
    private Metadata metadata;

    /**
     * SSE 消息载荷。
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private ChatMessageVO message;
        private String statusText;
        private String workflowInstanceId;
        private String nodeKey;
        private String nodeName;
        private String stepStatus;
        private String content;
        private String approvalRecordId;
        private String approvalStatus;
        private Boolean done;
    }

    /**
     * SSE 消息元数据。
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        private String chatMessageId;
        private String workflowInstanceId;
        private String stepId;
        private String approvalRecordId;
    }

    /**
     * SSE 消息类型枚举。
     */
    public enum Type {
        AI_GENERATED_CONTENT,
        AI_PLANNING,
        AI_THINKING,
        AI_EXECUTING,
        AI_DONE,
        WORKFLOW_STARTED,
        STEP_STARTED,
        STEP_DELTA,
        STEP_COMPLETED,
        STEP_FAILED,
        APPROVAL_REQUIRED,
        WORKFLOW_RESUMED,
        WORKFLOW_REJECTED,
        WORKFLOW_FINISHED,
        WORKFLOW_FAILED,
    }
}
