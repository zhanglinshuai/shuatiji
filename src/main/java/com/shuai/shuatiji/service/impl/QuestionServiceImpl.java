package com.shuai.shuatiji.service.impl;

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
import com.shuai.shuatiji.model.dto.question.QuestionQueryRequest;
import com.shuai.shuatiji.model.entity.Question;
import com.shuai.shuatiji.model.entity.QuestionBankQuestion;
import com.shuai.shuatiji.model.entity.User;
import com.shuai.shuatiji.model.esdao.QuestionEsDTO;
import com.shuai.shuatiji.model.vo.QuestionVO;
import com.shuai.shuatiji.model.vo.UserVO;
import com.shuai.shuatiji.service.QuestionBankQuestionService;
import com.shuai.shuatiji.service.QuestionService;
import com.shuai.shuatiji.service.UserService;
import com.shuai.shuatiji.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;


    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = question.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }


    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);
        // 查询数据库
        Long questionId = questionQueryRequest.getQuestionId();
        if (questionId != null) {
            LambdaQueryWrapper<QuestionBankQuestion> questionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .select(QuestionBankQuestion::getQuestionId)
                    .eq(QuestionBankQuestion::getQuestionId, questionId);
            List<QuestionBankQuestion> list = questionBankQuestionMapper.selectList(questionLambdaQueryWrapper);
            List<Long> questionList = list.stream()
                    .map(QuestionBankQuestion::getQuestionId)
                    .collect(Collectors.toList());
            queryWrapper.in("id", questionList);
        }
        Page<Question> page = this.page(new Page<>(current, size), queryWrapper);
        return page;

    }

    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        //获取参数
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String searchText = questionQueryRequest.getSearchText();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();
        Long questionId = questionQueryRequest.getQuestionId();
        int current = questionQueryRequest.getCurrent() - 1;
        int pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        //获取查询对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //构造查询条件
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
        if (id != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
        }
        if (notId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("notId", notId));
        }
        if (userId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
        }
        if (questionId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("questionId", questionId));
        }
        //必须包含所有标签
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }
        //按关键词检索，只要有一个匹配就能找到
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.filter(QueryBuilders.matchQuery("title", searchText));
            boolQueryBuilder.filter(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.filter(QueryBuilders.matchQuery("answer", searchText));
        }
        //排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if (StringUtils.isNotBlank(sortField)) {
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortField) ? SortOrder.ASC : SortOrder.DESC);
        }
        //分页
        PageRequest pageRequest = PageRequest.of(current, pageSize);
        //构造查询函数
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder)
                .build();
        SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(query, QuestionEsDTO.class);
        //复用mysql的分页对象,封装返回结果
        Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        ArrayList<Question> list = new ArrayList<>();
        if (searchHits.hasSearchHits()) {
            List<SearchHit<QuestionEsDTO>> searchHitList = searchHits.getSearchHits();
            for (SearchHit<QuestionEsDTO> searchHit : searchHitList) {
                list.add(QuestionEsDTO.dtoToObj(searchHit.getContent()));
            }
        }
        page.setRecords(list);
        return page;
    }

    @Override
    public void batchDeleteQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR);
        for (Long questionId : questionIdList) {
            //从题目中删除信息
            boolean result = this.removeById(questionId);
            if (!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            LambdaQueryWrapper<QuestionBankQuestion> questionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId);
            //从题库中删除题目信息
            int delete = questionBankQuestionMapper.delete(questionLambdaQueryWrapper);
            if (delete<0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        }
    }
}
