package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.model.entity.ExecutionObservation;

import java.util.List;

public interface ObservationStore {
    void create(ExecutionObservation observation);

    void update(ExecutionObservation observation);

    List<ExecutionObservation> findByWorkflowInstanceId(String workflowInstanceId);
}
