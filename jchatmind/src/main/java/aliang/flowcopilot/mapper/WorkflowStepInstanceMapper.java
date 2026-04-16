package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.WorkflowStepInstance;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowStepInstanceMapper {
    int insert(WorkflowStepInstance workflowStepInstance);

    List<WorkflowStepInstance> selectByWorkflowInstanceId(String workflowInstanceId);

    int updateById(WorkflowStepInstance workflowStepInstance);
}
