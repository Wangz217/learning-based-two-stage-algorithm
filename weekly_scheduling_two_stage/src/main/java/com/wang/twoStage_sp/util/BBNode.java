package com.wang.twoStage_sp.util;

import com.wang.param;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


import com.wang.enums.SolStatus;
import com.wang.param.*;

import gurobi.GRBEnv;
import gurobi.GRBException;

import java.util.*;

/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang.twoStage_sp.util
 * Version: 1.0
 */
@Slf4j
@Data
public class BBNode {
    private GRBEnv env;
    private CG_RMP rmp;
    private CG_SP sp;
    private int ID;
    private Map<String, List<Integer>> mustInclude;  // 主问题必须包含的约束
    private Map<String, List<Integer>> mustExclude;  // 主问题必须排除的排班

    public BBNode clone(int id) {
        BBNode nodeCloned = null;
        try {
            nodeCloned = new BBNode(env, id);
        } catch (GRBException e) {
            e.printStackTrace();
        }
        nodeCloned.rmp = rmp.clone();
        nodeCloned.sp = sp.clone();
        nodeCloned.setMustInclude(Copier.copyMapOfList(mustInclude));
        nodeCloned.setMustExclude(Copier.copyMapOfList(mustExclude));
        return nodeCloned;
    }

    public BBNode(GRBEnv env, int id) throws GRBException {
        ID = id;
        this.env = env;
        mustInclude = new HashMap<>();
        mustExclude = new HashMap<>();

        mustInclude.put(String.valueOf(id), new ArrayList<Integer>());
        mustExclude.put(String.valueOf(id), new ArrayList<Integer>());

        rmp = new CG_RMP(env);
        // 初始化默认的几个排班
        rmp.addInitShifts();
        rmp.initModel();
        sp = new CG_SP(env);
    }

    public List<List<Double>> getSol() throws GRBException {
        return rmp.getSol();
    }

    public double getObj() {
        return rmp.getObjVal();
    }

    public double[] getTWD() {
        return rmp.getTwd();
    }

    public void resetP(double[] p_t) {
        rmp.resetP(p_t);
    }

    public SolStatus solve() throws GRBException {
        //列生成算法
        int iterCount = 0;
        boolean canImprove;
        do {
            canImprove = false;
            iterCount++;
            log.debug("iteration: {} ...", iterCount);
            if (rmp.solve() == SolStatus.INFEASIBLE) {
                log.warn("Infeasible RMP for column generation.");
                return SolStatus.INFEASIBLE;
            }
            log.debug("CG-RMP OBJ: {}", rmp.getObjVal());

            sp.setPrices(rmp.getMinPhysicianDuals(),rmp.getMaxPhysicianDuals());
//            sp.solveWithlabelling();
//            if (SolStatus.INFEASIBLE == sp.solveWithlabelling()) continue;
            sp.initSPModel();
            if (SolStatus.INFEASIBLE == sp.solveWithGurobi()) continue;
            log.debug("schedule: {}", sp.getSchedule().toString());
            log.debug("schedule length: {}", sp.getScheduleLen());
            log.debug("CG-SP obj: {}", sp.getObjVal() );


            if (sp.getObjVal() <= -param.err) {
                canImprove = true;
                rmp.addCol(sp.getSchedule(), sp.getScheduleLen());
            }
            else{
                log.debug("CG-SP cannot make further improvement");
            }
        } while (canImprove);
        return SolStatus.OPTIMAL;
    }

    /**
     * 找到分支点j
     */
    public int findBranchPoint() throws GRBException {
        int branchPoint = rmp.findBranchPoint();
        return branchPoint;
    }

}