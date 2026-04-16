package aliang.flowcopilot.mapper;

import aliang.flowcopilot.model.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识库表访问接口。
 * <p>
 * 负责知识库元数据的增删改查，并支持按 ID 批量读取知识库配置。
 */
@Mapper
public interface KnowledgeBaseMapper {
    /**
     * 插入知识库。
     *
     * @param knowledgeBase 知识库实体
     * @return 受影响行数
     */
    int insert(KnowledgeBase knowledgeBase);

    /**
     * 按主键查询知识库。
     *
     * @param id 知识库主键
     * @return 知识库实体
     */
    KnowledgeBase selectById(String id);

    /**
     * 查询全部知识库。
     *
     * @return 知识库列表
     */
    List<KnowledgeBase> selectAll();

    /**
     * 按主键集合批量查询知识库。
     *
     * @param ids 知识库 ID 列表
     * @return 知识库列表
     */
    List<KnowledgeBase> selectByIdBatch(List<String> ids);

    /**
     * 删除知识库。
     *
     * @param id 知识库主键
     * @return 受影响行数
     */
    int deleteById(String id);

    /**
     * 更新知识库。
     *
     * @param knowledgeBase 知识库实体
     * @return 受影响行数
     */
    int updateById(KnowledgeBase knowledgeBase);
}
