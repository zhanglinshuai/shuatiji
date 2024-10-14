package com.shuai.shuatiji.job.once;

import cn.hutool.core.collection.CollUtil;
import com.shuai.shuatiji.esdao.QuestionEsDao;
import com.shuai.shuatiji.model.entity.Question;
import com.shuai.shuatiji.model.esdao.QuestionEsDTO;
import com.shuai.shuatiji.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全量从数据库同步question到es当中
 */


@Slf4j
@Component
//使用CommandLineRunner可以使任务只执行一次
public class FullSyncQuesitonToEs implements CommandLineRunner {

    @Resource
    private QuestionService questionService;

    @Resource

    private QuestionEsDao questionEsDao;


    @Override
    public void run(String... args) throws Exception {
        //全量获取题目
        List<Question> list = questionService.list();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        //将question转换成questionEsDTO
        List<QuestionEsDTO> questionEsDTOList = list.stream()
                .map(QuestionEsDTO::objToDto)
                .collect(Collectors.toList());
        //分页批量插入es当中
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("FullSyncQuestionToES start, total{}",total);
        for(int i = 0 ; i < total ; i+=pageSize){
            //数据同步的下标不能超过总数
            int end = Math.min(i+pageSize, total);
            log.info("Sync {} to {}", i, end);
            questionEsDao.saveAll(questionEsDTOList.subList(i,end));
        }
        log.info("FullSyncQuestionToES end, total{}",total);
    }
}
