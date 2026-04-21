package com.omnisource.service;

import com.omnisource.dto.response.RagRetrievalResponse;

import java.util.List;

/**
 * RAG检索服务接口。
 */
public interface RagService {

    /**
     * 根据问题检索相关知识。
     *
     * @param question 用户问题
     * @param topK 返回条数
     * @return 检索结果列表
     */
    List<RagRetrievalResponse> retrieve(String question, int topK);

    /**
     * 根据问题检索相关知识，使用默认返回条数。
     *
     * @param question 用户问题
     * @return 检索结果列表
     */
    default List<RagRetrievalResponse> retrieve(String question) {
        return retrieve(question, 3);
    }

    /**
     * 将检索结果拼装成可直接注入 Prompt 的上下文。
     *
     * @param question 用户问题
     * @param topK 返回条数
     * @return 拼装后的上下文
     */
    String buildContext(String question, int topK);

    /**
     * 将检索结果拼装成可直接注入 Prompt 的上下文，使用默认返回条数。
     *
     * @param question 用户问题
     * @return 拼装后的上下文
     */
    default String buildContext(String question) {
        return buildContext(question, 3);
    }

    /**
     * 重新加载知识库。
     */
    void reload();

    /**
     * 知识库是否已加载。
     *
     * @return 是否可用
     */
    boolean isReady();
}