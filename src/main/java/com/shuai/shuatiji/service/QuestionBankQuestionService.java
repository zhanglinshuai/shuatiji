package com.shuai.shuatiji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shuai.shuatiji.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.shuai.shuatiji.model.entity.QuestionBankQuestion;
import com.shuai.shuatiji.model.entity.User;
import com.shuai.shuatiji.model.vo.QuestionBankQuestionVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题库题目关联表服务

 */
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add 对创建的数据进行校验
     */
    void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest);
    
    /**
     * 获取题库题目关联表封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request);

    /**
     * 分页获取题库题目关联表封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request);

    /**
     * 批量将题目添加到题库当中
     * @param questionIdList
     * @param questionBankId
     * @param user
     */
    void batchAddAllQuestionToBank(List<Long> questionIdList, Long questionBankId, User user);

    /**
     * 批量从题库中移除题目
     * @param questionIdList
     * @param questionBankId
     * @param user
     */
    void batchDeleteAllQuestionFromBank(List<Long> questionIdList, Long questionBankId, User user);
    /**
     * 批量添加题目到题库（事务，仅供内部调用）
     * @param questionBankQuestionList
     */
    void batchAddAllQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestionList);

    }
