package com.kama.jchatmind.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.InputStream;
import java.util.List;

/**
 * Markdown 解析服务接口。
 * <p>
 * 负责从 Markdown 输入流中抽取结构化章节，为后续分块和向量化提供基础数据。
 */
public interface MarkdownParserService {
    /**
     * 解析 Markdown 文件，提取标题和对应内容。
     *
     * @param inputStream Markdown 文件输入流
     * @return 章节列表，每个元素包含标题和该标题下的正文
     */
    List<MarkdownSection> parseMarkdown(InputStream inputStream);
    
    /**
     * Markdown 章节值对象。
     * <p>
     * 一个章节由标题和正文两部分组成，是文档切块过程中的中间表示。
     */
    @Data
    @AllArgsConstructor
    @ToString
    class MarkdownSection {
        private String title;
        private String content;
    }
}
