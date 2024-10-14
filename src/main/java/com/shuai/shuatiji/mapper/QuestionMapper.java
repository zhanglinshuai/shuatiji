package com.shuai.shuatiji.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuai.shuatiji.model.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author 86175
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2024-10-08 11:28:04
* @Entity generator.domain.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     *
     * @param minUpdateTime
     * @return
     */
    @Select("select * from question where updateTime>=#{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);
}




