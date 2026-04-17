package aliang.flowcopilot.workflow.event;

import aliang.flowcopilot.message.SseMessage;
import aliang.flowcopilot.model.entity.WorkflowStepInstance;
import aliang.flowcopilot.service.SseService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publishes workflow execution events through the existing SSE channel.
 */
@Slf4j
@Service
@AllArgsConstructor
public class WorkflowEventPublisher {

    private final SseService sseService;

    public void workflowStarted(String workflowInstanceId, String title) {
        send(workflowInstanceId, SseMessage.Type.WORKFLOW_STARTED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .statusText("工作流已启动：" + title)
                .done(false)
                .build(), null);
    }

    public void workflowResumed(String workflowInstanceId, String message) {
        send(workflowInstanceId, SseMessage.Type.WORKFLOW_RESUMED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .statusText(message)
                .done(false)
                .build(), null);
    }

    public void stepStarted(String workflowInstanceId, WorkflowStepInstance step) {
        send(workflowInstanceId, SseMessage.Type.STEP_STARTED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(step.getNodeKey())
                .nodeName(step.getNodeName())
                .stepStatus(step.getStatus())
                .statusText(step.getNodeName() + " 开始执行")
                .done(false)
                .build(), step.getId());
    }

    public void stepCompleted(String workflowInstanceId, WorkflowStepInstance step, String content) {
        streamContent(workflowInstanceId, step, content);
        send(workflowInstanceId, SseMessage.Type.STEP_COMPLETED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(step.getNodeKey())
                .nodeName(step.getNodeName())
                .stepStatus("COMPLETED")
                .content(content)
                .statusText(step.getNodeName() + " 执行完成")
                .done(false)
                .build(), step.getId());
    }

    public void stepFailed(String workflowInstanceId, WorkflowStepInstance step, String errorMessage) {
        send(workflowInstanceId, SseMessage.Type.STEP_FAILED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(step.getNodeKey())
                .nodeName(step.getNodeName())
                .stepStatus("FAILED")
                .content(errorMessage)
                .statusText(step.getNodeName() + " 执行失败")
                .done(false)
                .build(), step.getId());
    }

    public void approvalRequired(String workflowInstanceId, String approvalRecordId, String content) {
        send(workflowInstanceId, SseMessage.Type.APPROVAL_REQUIRED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .approvalRecordId(approvalRecordId)
                .approvalStatus("PENDING")
                .content(content)
                .statusText("需要人工审批")
                .done(false)
                .build(), null, approvalRecordId);
    }

    public void workflowFinished(String workflowInstanceId, String content) {
        send(workflowInstanceId, SseMessage.Type.WORKFLOW_FINISHED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .content(content)
                .statusText("工作流执行完成")
                .done(true)
                .build(), null);
    }

    public void workflowFailed(String workflowInstanceId, String errorMessage) {
        send(workflowInstanceId, SseMessage.Type.WORKFLOW_FAILED, SseMessage.Payload.builder()
                .workflowInstanceId(workflowInstanceId)
                .content(errorMessage)
                .statusText("工作流执行失败")
                .done(true)
                .build(), null);
    }

    private void streamContent(String workflowInstanceId, WorkflowStepInstance step, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        int chunkSize = 160;
        for (int start = 0; start < content.length(); start += chunkSize) {
            String chunk = content.substring(start, Math.min(start + chunkSize, content.length()));
            send(workflowInstanceId, SseMessage.Type.STEP_DELTA, SseMessage.Payload.builder()
                    .workflowInstanceId(workflowInstanceId)
                    .nodeKey(step.getNodeKey())
                    .nodeName(step.getNodeName())
                    .stepStatus("STREAMING")
                    .content(chunk)
                    .statusText(step.getNodeName() + " 正在输出")
                    .done(false)
                    .build(), step.getId());
        }
    }

    private void send(String workflowInstanceId, SseMessage.Type type, SseMessage.Payload payload, String stepId) {
        send(workflowInstanceId, type, payload, stepId, null);
    }

    private void send(String workflowInstanceId, SseMessage.Type type, SseMessage.Payload payload, String stepId, String approvalRecordId) {
        try {
            sseService.send(workflowInstanceId, SseMessage.builder()
                    .type(type)
                    .payload(payload)
                    .metadata(SseMessage.Metadata.builder()
                            .workflowInstanceId(workflowInstanceId)
                            .stepId(stepId)
                            .approvalRecordId(approvalRecordId)
                            .build())
                    .build());
        } catch (Exception e) {
            log.debug("Workflow SSE client is not connected yet: {}", workflowInstanceId);
        }
    }
}
