package com.wang;

import com.wang.enums.SolStatus;
import gurobi.GRBEnv;
import gurobi.GRBException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


/**
 * Hello world!
 *
 */
@Slf4j
public class App 
{    private GRBEnv env;
    private MP mp;

    private double Obj;   // 最优目标值
    private int[] Sol;  //最优解

    public App (GRBEnv env, int expNum) throws GRBException {
        this.env = env;
        mp = new MP(env,0, expNum);
    }

    public void solveWithGurobi() throws GRBException, IOException {
        Obj = Double.MAX_VALUE;
        int iteration = 0;

        log.info("weekly scheduling model");

        log.info("\n -------- MP --------\n");
        if (mp.solve() == SolStatus.INFEASIBLE)
            log.error("no solution to MP!");
        log.info("mp obj: {}", mp.getObj());
        mp.getSol();
        saveResult(mp);

    }

    private void saveResult(MP mp) throws IOException {
        log.info("Generated " + mp.getGenCutCount() + " in total.");
        mp.saveResult();
    }



    public static void main ( String[] args ) throws GRBException, IOException
    {
        /** 获取当前系统时间*/
        long startTime =  System.currentTimeMillis();

        for(int i = 0; i < 1000; i++) {
            int exp = i;
            log.info( i+1 + "th. exp");
            GRBEnv env = new GRBEnv();
            App weekly_schedule = new App(env, exp);
            weekly_schedule.solveWithGurobi();

//            if((i+1) % 100 == 0 ){
//                /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/
//                long endTime =  System.currentTimeMillis();
//                long usedTime = (endTime-startTime)/1000/3600;
//                log.info( i+1 + "exps, usedTime: " + usedTime + " hours.");
//            }
        }

        long endTime =  System.currentTimeMillis();
        long usedTime = (endTime-startTime)/1000;
        log.info("usedTime: " + usedTime + " seconds.");

    }
}
