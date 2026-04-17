package aliang.flowcopilot.model.entity;

import java.time.LocalDateTime;
import java.util.Arrays;

import lombok.Builder;
import lombok.Data;

/**
 * 向量分块数据库实体。
 * <p>
 * 对应 `chunk_bge_m3` 表，保存文档切块后的文本内容及其 embedding，
 * 是知识库 RAG 检索的核心数据结构。
 */
@Data
@Builder
public class ChunkBgeM3 {
    /**
     * 分块主键 ID。
     */
    private String id;

    /**
     * 所属知识库 ID。
     */
    private String kbId;

    /**
     * 所属文档 ID。
     */
    private String docId;

    /**
     * 分块正文内容。
     */
    private String content;

    /**
     * 分块扩展元数据，JSON 字符串格式。
     */
    private String metadata;

    /**
     * 分块向量表示。
     */
    private float[] embedding;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ChunkBgeM3 other = (ChunkBgeM3) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getKbId() == null ? other.getKbId() == null : this.getKbId().equals(other.getKbId()))
            && (this.getDocId() == null ? other.getDocId() == null : this.getDocId().equals(other.getDocId()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getMetadata() == null ? other.getMetadata() == null : this.getMetadata().equals(other.getMetadata()))
            && (this.getEmbedding() == null ? other.getEmbedding() == null : Arrays.equals(this.getEmbedding(), other.getEmbedding()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getKbId() == null) ? 0 : getKbId().hashCode());
        result = prime * result + ((getDocId() == null) ? 0 : getDocId().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getMetadata() == null) ? 0 : getMetadata().hashCode());
        result = prime * result + ((getEmbedding() == null) ? 0 : Arrays.hashCode(getEmbedding()));
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", id=" + id +
                ", kbId=" + kbId +
                ", docId=" + docId +
                ", content=" + content +
                ", metadata=" + metadata +
                ", embedding=" + Arrays.toString(embedding) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                "]";
    }
}
