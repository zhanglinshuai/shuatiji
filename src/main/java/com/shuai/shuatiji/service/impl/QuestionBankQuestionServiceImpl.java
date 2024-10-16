package com.shuai.shuatiji.service.impl;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shuai.shuatiji.common.ErrorCode;
import com.shuai.shuatiji.constant.CommonConstant;
import com.shuai.shuatiji.exception.BusinessException;
import com.shuai.shuatiji.exception.ThrowUtils;
import com.shuai.shuatiji.mapper.QuestionBankQuestionMapper;
import com.shuai.shuatiji.mapper.QuestionMapper;
import com.shuai.shuatiji.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.shuai.shuatiji.model.entity.Question;
import com.shuai.shuatiji.model.entity.QuestionBank;
import com.shuai.shuatiji.model.entity.QuestionBankQuestion;
import com.shuai.shuatiji.model.entity.User;
import com.shuai.shuatiji.model.vo.QuestionBankQuestionVO;
import com.shuai.shuatiji.model.vo.UserVO;
import com.shuai.shuatiji.service.QuestionBankQuestionService;
import com.shuai.shuatiji.service.QuestionBankService;
import com.shuai.shuatiji.service.QuestionService;
import com.shuai.shuatiji.service.UserService;
import com.shuai.shuatiji.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题库题目关联表服务实现
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private QuestionService questionService;

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add                  对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        Long questionId = questionBankQuestion.getQuestionId();

        if (questionId != null) {
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
            }
        }
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        if (questionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            if (questionBank == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库不存在");
            }
        }

    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 精确查询
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目关联表封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);
        // endregion

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题库题目关联表封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    @Override
    public void batchAddAllQuestionToBank(List<Long> questionIdList, Long questionBankId, User user) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(questionBankId == null, ErrorCode.PARAMS_ERROR, "题库id非法");
        //查看传来的id是否合法，在数据库中是否都存在
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper = Wrappers.lambdaQuery(Question.class)
                .select(Question::getId)
                .in(Question::getId, questionIdList);
        List<Long> validQuestionIdList = questionService.listObjs(questionLambdaQueryWrapper, obj -> (Long) obj);
        //获取合法的id
        //检查哪些题目还不存在于题库中，避免重复插入
        LambdaQueryWrapper<QuestionBankQuestion> queryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, questionIdList);
        List<QuestionBankQuestion> list = this.list(queryWrapper);
        //已存在于题库的题目id
        Set<Long> existQuestionIdSet = list.stream().
                map(QuestionBankQuestion::getId)
                .collect(Collectors.toSet());
        //已存在于题目的id，不需要重复添加
        validQuestionIdList = validQuestionIdList.stream().filter(questionId -> {
            return !existQuestionIdSet.contains(questionId);
        }).collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR);
        //分批处理，避免长事务，假设每次处理1000条数据

        //创建自定义线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                20,
                50,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(10000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        //创建数组，保存所有任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int batchSize = 1000;
        int totalQuestionListSize = validQuestionIdList.size();
        for (int i = 0; i < totalQuestionListSize; i+=batchSize) {
            List<Long> subList = validQuestionIdList.subList(i, Math.min(i + batchSize, totalQuestionListSize));
            List<QuestionBankQuestion> questionBankQuestions = subList.stream().map(questionId -> {
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setId(questionId);
                questionBankQuestion.setQuestionBankId(questionBankId);
                questionBankQuestion.setUserId(user.getId());
                return questionBankQuestion;
            }).collect(Collectors.toList());
            //使用事务处理每批数据
            //如果想要在spring调用使用了@Transactional注解的方法，就要使用代理对象，使用this，事务会失效
            QuestionBankQuestionServiceImpl questionBankQuestionService = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();

            //分批执行任务
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                questionBankQuestionService.batchAddAllQuestionToBankInner(questionBankQuestions);
            }, threadPoolExecutor).exceptionally(ex -> {
                log.error("任务执行失败{}", ex.getMessage());
                return null;
            });
            futures.add(future);

        }
        //等所有批次完成操作
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        //关闭线程池
        threadPoolExecutor.shutdown();
    }

    /**
     * 批量添加题目到题库（事务，仅供内部调用）
     *
     * @param questionBankQuestionList
     */
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void batchAddAllQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestionList) {
        try {
            boolean save = this.saveBatch(questionBankQuestionList);
            if (!save) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        } catch (DataIntegrityViolationException e) {
            log.error("数据库唯一键冲突或违反其他完整性约束,错误信息{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
        } catch (DataAccessException e) {
            log.error("数据库连接问题，事务问题等导致操作失败,错误信息{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
        } catch (Exception e) {
            log.error("添加题目到数据库的时候发生未知错误,错误信息{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库中添加信息失败");
        }

    }

    @Override
    public void batchDeleteAllQuestionFromBank(List<Long> questionIdList, Long questionBankId, User user) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(questionBankId == null, ErrorCode.PARAMS_ERROR);
        for (Long questionId : questionIdList) {
            LambdaQueryWrapper<QuestionBankQuestion> questionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            boolean remove = this.remove(questionLambdaQueryWrapper);
            if (!remove) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        }
    }

}
