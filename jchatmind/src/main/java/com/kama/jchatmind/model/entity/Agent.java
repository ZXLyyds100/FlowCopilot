package com.kama.jchatmind.model.entity;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Agent 数据库实体。
 * <p>
 * 对应 `agent` 表，保存一个智能体的静态配置。
 * 其中工具列表、知识库列表和聊天参数以 JSON 字符串形式存储。
 */
@Data
@Builder
public class Agent {
    /**
     * Agent 主键 ID。
     */
    private String id;

    /**
     * Agent 名称。
     */
    private String name;

    /**
     * Agent 描述信息。
     */
    private String description;

    /**
     * Agent 系统提示词。
     */
    private String systemPrompt;

    /**
     * Agent 默认使用的模型名称。
     */
    private String model;

    /**
     * 允许使用的工具列表，JSON 字符串格式。
     */
    private String allowedTools;

    /**
     * 允许访问的知识库 ID 列表，JSON 字符串格式。
     */
    private String allowedKbs;

    /**
     * 聊天参数配置，JSON 字符串格式。
     */
    private String chatOptions;

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
        Agent other = (Agent) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
                && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
                && (this.getSystemPrompt() == null ? other.getSystemPrompt() == null : this.getSystemPrompt().equals(other.getSystemPrompt()))
                && (this.getModel() == null ? other.getModel() == null : this.getModel().equals(other.getModel()))
                && (this.getAllowedTools() == null ? other.getAllowedTools() == null : this.getAllowedTools().equals(other.getAllowedTools()))
                && (this.getAllowedKbs() == null ? other.getAllowedKbs() == null : this.getAllowedKbs().equals(other.getAllowedKbs()))
                && (this.getChatOptions() == null ? other.getChatOptions() == null : this.getChatOptions().equals(other.getChatOptions()))
                && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
                && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getSystemPrompt() == null) ? 0 : getSystemPrompt().hashCode());
        result = prime * result + ((getModel() == null) ? 0 : getModel().hashCode());
        result = prime * result + ((getAllowedTools() == null) ? 0 : getAllowedTools().hashCode());
        result = prime * result + ((getAllowedKbs() == null) ? 0 : getAllowedKbs().hashCode());
        result = prime * result + ((getChatOptions() == null) ? 0 : getChatOptions().hashCode());
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
                ", name=" + name +
                ", description=" + description +
                ", systemPrompt=" + systemPrompt +
                ", model=" + model +
                ", allowedTools=" + allowedTools +
                ", allowedKbs=" + allowedKbs +
                ", chatOptions=" + chatOptions +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                "]";
    }
}
