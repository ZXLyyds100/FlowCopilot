package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.ChunkBgeM3;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 向量分块表访问接口。
 * <p>
 * 负责文档分块的持久化、更新、删除，以及基于 pgvector 的相似度检索。
 */
@Mapper
public interface ChunkBgeM3Mapper {
    /**
     * 插入一个向量分块。
     *
     * @param chunkBgeM3 分块实体
     * @return 受影响行数
     */
    int insert(ChunkBgeM3 chunkBgeM3);

    /**
     * 按主键查询分块。
     *
     * @param id 分块主键
     * @return 分块实体
     */
    ChunkBgeM3 selectById(String id);

    /**
     * 删除分块。
     *
     * @param id 分块主键
     * @return 受影响行数
     */
    int deleteById(String id);

    /**
     * 更新分块。
     *
     * @param chunkBgeM3 分块实体
     * @return 受影响行数
     */
    int updateById(ChunkBgeM3 chunkBgeM3);

    /**
     * 在指定知识库下执行向量相似度检索。
     *
     * @param kbId 知识库 ID
     * @param vectorLiteral PostgreSQL vector literal 字符串
     * @param limit 返回结果数
     * @return 相似分块列表
     */
    List<ChunkBgeM3> similaritySearch(
            @Param("kbId") String kbId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("limit") int limit
    );
}
