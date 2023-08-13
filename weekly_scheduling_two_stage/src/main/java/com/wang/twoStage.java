package com.wang;

import com.wang.twoStage_mp.MP;
import com.wang.twoStage_sp.SPBranchNPrice;
import com.wang.enums.SolStatus;

import gurobi.GRBEnv;
import gurobi.GRBException;

import java.io.IOException;
import java.util.Arrays;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang
 * Version: 1.0
 */
@Slf4j
public class twoStage {


    private GRBEnv env;
    private MP mp;
    private SPBranchNPrice sp;
    private double logicBasedObj;   // logic based 最新的最优目标值
    private int[] logicBasedSol;  //logic based 最新的最优解

    public twoStage(GRBEnv env) throws GRBException {
        this.env = env;
        mp = new MP(env);
    }

    /**
     * First phase : staffing
     * Second phase: scheduling
     * Output: ub, lb, obj
     * @ throws GRBException
     * @ throws IOException
     */
    public void twoPhaseLogicBased() throws GRBException, IOException {
        logicBasedObj = Double.MAX_VALUE;
        int iteration = 0;

        log.info("two phase logic-based algorithm start");

        log.info("\n -------- MP --------\n");
        if (mp.solve() == SolStatus.INFEASIBLE)
            log.error("no solution to MP!");
        log.info("mp obj: {}", mp.getObj());
        sp = new SPBranchNPrice(env);       // 重新初始化SP
        log.info("\n -------- SP --------\n");
        if (SolStatus.INFEASIBLE == sp.solve(mp.getSol())) {
            log.error("no solution to SP!");
        }
        if (sp.getObj() < logicBasedObj) {    // 更新本节点的上界
            logicBasedSol = sp.getStaffing();
            logicBasedObj = sp.getObj();
        }
        saveResult(mp, sp);
        System.out.println("lb: " + mp.getObj() + ", ub: " + sp.getObj());
        if (Math.abs(mp.getObj() - sp.getObj()) <= param.err) {
            System.out.println("optimal val: " + logicBasedObj);
            return;
        }
    }

    private void saveResult(MP mp, SPBranchNPrice sp) throws IOException {
        log.info("Generated " + mp.getGenCutCount() + " in total.");
        mp.saveResult();
        sp.saveResult();
    }

    //	程序运行入口
    public static void main(String[] args) throws GRBException, IOException {

        /** 获取当前系统时间*/
        long startTime =  System.currentTimeMillis();

        /** 程序运行*/
        GRBEnv env = new GRBEnv();
        twoStage two_stage = new twoStage(env);
        two_stage.twoPhaseLogicBased();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/
        long endTime =  System.currentTimeMillis();
        long usedTime = (endTime-startTime)/1000;
        log.info("usedTime: " + usedTime + " seconds in total.");
    }
}
