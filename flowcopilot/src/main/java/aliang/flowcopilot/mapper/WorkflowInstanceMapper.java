package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.WorkflowInstance;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowInstanceMapper {
    int insert(WorkflowInstance workflowInstance);

    WorkflowInstance selectById(String id);

    List<WorkflowInstance> selectRecent(int limit);

    int updateById(WorkflowInstance workflowInstance);
}
