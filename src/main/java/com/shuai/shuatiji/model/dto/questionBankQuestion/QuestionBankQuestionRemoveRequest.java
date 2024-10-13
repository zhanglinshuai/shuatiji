package com.shuai.shuatiji.model.dto.questionBankQuestion;

import lombok.Data;

import java.io.Serializable;

/**
 * 题库题目关联移除请求参数
 */
@Data
public class QuestionBankQuestionRemoveRequest implements Serializable {

    private static final long serialVersionUID = 1104145420238876282L;

    private Long questionBankId;
    private Long questionId;
}
