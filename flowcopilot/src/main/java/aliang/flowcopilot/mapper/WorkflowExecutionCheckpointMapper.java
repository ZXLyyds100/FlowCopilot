package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.WorkflowExecutionCheckpoint;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowExecutionCheckpointMapper {
    int insert(WorkflowExecutionCheckpoint checkpoint);

    List<WorkflowExecutionCheckpoint> selectByWorkflowInstanceId(String workflowInstanceId);

    WorkflowExecutionCheckpoint selectLatestByWorkflowInstanceId(String workflowInstanceId);

    WorkflowExecutionCheckpoint selectLatestByWorkflowAndNode(String workflowInstanceId, String nodeKey, String checkpointType);
}
