package aliang.flowcopilot.model.entity;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * 文档数据库实体。
 * <p>
 * 对应 `document` 表，记录知识库中上传或登记的文档元信息。
 */
@Data
@Builder
public class Document {
    /**
     * 文档主键 ID。
     */
    private String id;

    /**
     * 所属知识库 ID。
     */
    private String kbId;

    /**
     * 原始文件名。
     */
    private String filename;

    /**
     * 文件类型，例如 md、pdf、txt。
     */
    private String filetype;

    /**
     * 文件大小，单位字节。
     */
    private Long size;

    /**
     * 文档扩展元数据，JSON 字符串格式。
     * 例如文件存储路径、上传参数等。
     */
    private String metadata;

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
        Document other = (Document) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getKbId() == null ? other.getKbId() == null : this.getKbId().equals(other.getKbId()))
            && (this.getFilename() == null ? other.getFilename() == null : this.getFilename().equals(other.getFilename()))
            && (this.getFiletype() == null ? other.getFiletype() == null : this.getFiletype().equals(other.getFiletype()))
            && (this.getSize() == null ? other.getSize() == null : this.getSize().equals(other.getSize()))
            && (this.getMetadata() == null ? other.getMetadata() == null : this.getMetadata().equals(other.getMetadata()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getKbId() == null) ? 0 : getKbId().hashCode());
        result = prime * result + ((getFilename() == null) ? 0 : getFilename().hashCode());
        result = prime * result + ((getFiletype() == null) ? 0 : getFiletype().hashCode());
        result = prime * result + ((getSize() == null) ? 0 : getSize().hashCode());
        result = prime * result + ((getMetadata() == null) ? 0 : getMetadata().hashCode());
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
                ", filename=" + filename +
                ", filetype=" + filetype +
                ", size=" + size +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                "]";
    }
}
