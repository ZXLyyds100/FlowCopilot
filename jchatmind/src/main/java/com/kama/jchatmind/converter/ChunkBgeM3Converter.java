package com.kama.jchatmind.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.model.dto.ChunkBgeM3DTO;
import com.kama.jchatmind.model.entity.ChunkBgeM3;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 向量分块对象转换器。
 * <p>
 * 用于在 `chunk_bge_m3` 表实体和运行时 DTO 之间转换。
 */
@Component
@AllArgsConstructor
public class ChunkBgeM3Converter {

    private final ObjectMapper objectMapper;

    /**
     * 将分块 DTO 转换为数据库实体。
     *
     * @param chunkBgeM3DTO 分块 DTO
     * @return 分块实体
     * @throws JsonProcessingException 元数据序列化失败时抛出
     */
    public ChunkBgeM3 toEntity(ChunkBgeM3DTO chunkBgeM3DTO) throws JsonProcessingException {
        Assert.notNull(chunkBgeM3DTO, "ChunkBgeM3DTO cannot be null");

        return ChunkBgeM3.builder()
                .id(chunkBgeM3DTO.getId())
                .kbId(chunkBgeM3DTO.getKbId())
                .docId(chunkBgeM3DTO.getDocId())
                .content(chunkBgeM3DTO.getContent())
                .metadata(chunkBgeM3DTO.getMetadata() != null 
                        ? objectMapper.writeValueAsString(chunkBgeM3DTO.getMetadata()) 
                        : null)
                .embedding(chunkBgeM3DTO.getEmbedding())
                .createdAt(chunkBgeM3DTO.getCreatedAt())
                .updatedAt(chunkBgeM3DTO.getUpdatedAt())
                .build();
    }

    /**
     * 将分块实体转换为 DTO。
     *
     * @param chunkBgeM3 分块实体
     * @return 分块 DTO
     * @throws JsonProcessingException 元数据反序列化失败时抛出
     */
    public ChunkBgeM3DTO toDTO(ChunkBgeM3 chunkBgeM3) throws JsonProcessingException {
        Assert.notNull(chunkBgeM3, "ChunkBgeM3 cannot be null");

        return ChunkBgeM3DTO.builder()
                .id(chunkBgeM3.getId())
                .kbId(chunkBgeM3.getKbId())
                .docId(chunkBgeM3.getDocId())
                .content(chunkBgeM3.getContent())
                .metadata(chunkBgeM3.getMetadata() != null 
                        ? objectMapper.readValue(chunkBgeM3.getMetadata(), ChunkBgeM3DTO.MetaData.class) 
                        : null)
                .embedding(chunkBgeM3.getEmbedding())
                .createdAt(chunkBgeM3.getCreatedAt())
                .updatedAt(chunkBgeM3.getUpdatedAt())
                .build();
    }
}
