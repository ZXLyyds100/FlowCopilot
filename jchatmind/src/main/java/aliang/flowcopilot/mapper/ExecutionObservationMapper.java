package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.ExecutionObservation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExecutionObservationMapper {
    int insert(ExecutionObservation executionObservation);

    int updateById(ExecutionObservation executionObservation);

    List<ExecutionObservation> selectByWorkflowInstanceId(String workflowInstanceId);
}
