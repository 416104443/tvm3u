package io.kischang.fun.tvm3u;

import io.kischang.fun.tvm3u.controller.CleanAndReCacheJob;
import io.kischang.fun.tvm3u.controller.MainController;
import io.kischang.fun.tvm3u.init.HttpServerInit;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.tio.utils.jfinal.P;

/**
 * 程序入口 */
public class HttpServerTvM3uStarter {

    public static void main(String[] args) throws Exception {
        //启动服务
        startHttp();
        //调度任务
        startJob();
        //初始化一次
        MainController.reGetAndCache();
    }

    private static void startHttp() throws Exception {
        P.use("app.properties");
        HttpServerInit.init();
    }

    private static void startJob() throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(CleanAndReCacheJob.class)
                .withDescription("定时清理缓存功能")
                .withIdentity("Job-name", "Job-Group")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withDescription("每天晚上2点更新")
                .startNow()
                .withIdentity("Trigger-Name", "Trigger-Group")
                .withSchedule(
                        //间隔3天更新一次
                        CronScheduleBuilder.cronSchedule("0 0 2 1/3 * ? *")
                )
                .build();
        //创建一个调度器
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.scheduleJob(jobDetail,trigger);
    }

}
