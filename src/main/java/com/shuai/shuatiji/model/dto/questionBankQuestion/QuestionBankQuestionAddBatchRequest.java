package com.shuai.shuatiji.model.dto.questionBankQuestion;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量将题目添加到题库中的请求参数
 */
@Data
public class QuestionBankQuestionAddBatchRequest implements Serializable {

    private static final long serialVersionUID = -7339310940903515318L;

    private Long questionBankId;

    private List<Long> questionIdList;
}
