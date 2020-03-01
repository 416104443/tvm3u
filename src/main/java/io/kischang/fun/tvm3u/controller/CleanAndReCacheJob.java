package io.kischang.fun.tvm3u.controller;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 调度任务
 *
 * @author KisChang
 * @date 2020-03-01
 */
public class CleanAndReCacheJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //重新生成
        try {
            MainController.reGetAndCache();
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }
}
