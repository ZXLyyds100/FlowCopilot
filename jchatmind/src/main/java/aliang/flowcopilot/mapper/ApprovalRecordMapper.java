package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApprovalRecordMapper {
    int insert(ApprovalRecord approvalRecord);

    ApprovalRecord selectById(String id);

    ApprovalRecord selectPendingByWorkflowInstanceId(String workflowInstanceId);

    List<ApprovalRecord> selectByStatus(String status);

    int updateById(ApprovalRecord approvalRecord);
}
