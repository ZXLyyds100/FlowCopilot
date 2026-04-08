package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.Document;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 文档表访问接口。
 * <p>
 * 负责文档业务记录的增删改查，以及按知识库筛选文档。
 */
@Mapper
public interface DocumentMapper {
    /**
     * 插入文档记录。
     *
     * @param document 文档实体
     * @return 受影响行数
     */
    int insert(Document document);

    /**
     * 按主键查询文档。
     *
     * @param id 文档主键
     * @return 文档实体
     */
    Document selectById(String id);

    /**
     * 查询全部文档。
     *
     * @return 文档列表
     */
    List<Document> selectAll();

    /**
     * 按知识库查询文档列表。
     *
     * @param kbId 知识库 ID
     * @return 文档列表
     */
    List<Document> selectByKbId(String kbId);

    /**
     * 删除文档记录。
     *
     * @param id 文档主键
     * @return 受影响行数
     */
    int deleteById(String id);

    /**
     * 更新文档记录。
     *
     * @param document 文档实体
     * @return 受影响行数
     */
    int updateById(Document document);
}
