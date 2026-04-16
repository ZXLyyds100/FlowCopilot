package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.WorkflowDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowDefinitionMapper {
    int insert(WorkflowDefinition workflowDefinition);

    WorkflowDefinition selectById(String id);

    WorkflowDefinition selectByCode(String code);
}
