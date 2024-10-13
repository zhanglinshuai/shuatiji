package com.shuai.shuatiji.model.dto.questionBank;

import com.shuai.shuatiji.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询题库请求

 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionBankQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * id
     */
    private Long notId;
    /**
     * 描述
     */
    private String description;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 标题
     */
    private String title;
    /**
     * 图片
     */
    private String picture;
    /**
     * 是否要查询题目列表
     */
    private Boolean needQueryQuestionList;

    /**
     * 创建用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}