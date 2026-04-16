package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.mapper.ExecutionObservationMapper;
import aliang.flowcopilot.model.entity.ExecutionObservation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class DatabaseObservationStore implements ObservationStore {

    private final ExecutionObservationMapper executionObservationMapper;

    @Override
    public void create(ExecutionObservation observation) {
        executionObservationMapper.insert(observation);
    }

    @Override
    public void update(ExecutionObservation observation) {
        executionObservationMapper.updateById(observation);
    }

    @Override
    public List<ExecutionObservation> findByWorkflowInstanceId(String workflowInstanceId) {
        return executionObservationMapper.selectByWorkflowInstanceId(workflowInstanceId);
    }
}
