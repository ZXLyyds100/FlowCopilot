package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.Artifact;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ArtifactMapper {
    int insert(Artifact artifact);

    Artifact selectById(String id);

    List<Artifact> selectByWorkflowInstanceId(String workflowInstanceId);
}
