package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.ExecutionTraceRef;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExecutionTraceRefMapper {
    int insert(ExecutionTraceRef executionTraceRef);

    List<ExecutionTraceRef> selectByWorkflowInstanceId(String workflowInstanceId);
}
