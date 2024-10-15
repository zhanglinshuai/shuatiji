package com.shuai.shuatiji.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量将题目从题库中删除的请求参数
 */
@Data
public class QuestionDeleteBatchRequest implements Serializable {

    private static final long serialVersionUID = -7339310940903515318L;

    private List<Long> questionIdList;
}
