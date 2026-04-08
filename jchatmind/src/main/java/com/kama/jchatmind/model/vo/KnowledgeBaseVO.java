package com.kama.jchatmind.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 知识库前端展示对象。
 */
@Data
@Builder
public class KnowledgeBaseVO {
    private String id;
    private String name;
    private String description;
}
