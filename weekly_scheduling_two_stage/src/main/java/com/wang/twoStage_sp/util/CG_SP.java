package com.wang.twoStage_sp.util;

import com.wang.param;
import gurobi.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.wang.enums.SolStatus;
import com.wang.twoStage_sp.util.Coder;


import java.util.*;
import java.util.stream.Collectors;

import static com.wang.param.*;



/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang.twoStage_sp.util
 * Version: 1.0
 */
@Slf4j
@Data
public class CG_SP {
//    private double commonDualVals;  // common dual vals from RMP, 目标值当中减一次
    private double[] dualVals;  // dual vals pi_n from RMP
    private double dualVal;  // dual vals beta from RMP

    private double objVal;       // SP的最优值
    private List<Double> schedule;       // 生成的列，也就是SP的最优解
    private int scheduleLen;   // 生成的列的时长
    private int maxT;
    private HashMap<String, Double> objMemo;   // 动态规划最优值的备忘录，key为coded (v,w,n)，val为最小目标值
    private HashMap<String, List<Integer>> solMemo;   // 动态规划最优解的备忘录，key为coded (v,w,n)，val为最优解包含的时段
    private Set<Integer> mustInclude;
    private Set<Integer> mustExclude;

    private GRBEnv env;
    private GRBModel model;
    private GRBVar[] x_n;
    private int[] xn;


    public CG_SP() {

        objMemo = new HashMap<>();
        solMemo = new HashMap<>();

    }

    public CG_SP(GRBEnv env) {
        this.env = env;
//        objMemo = new HashMap<>();
//        solMemo = new HashMap<>();

//        schedule = new ArrayList<>();
//        scheduleLen = new int[];
//        p = new double[days*N];
//        minPhysicianDuals = new double[days*N];

        try {
            model = new GRBModel(env);
            model.set(GRB.IntParam.OutputFlag, outputMode);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public CG_SP(CG_SP other) {
        this.env = other.env;

        try {

            this.model = new GRBModel(this.env);
            this.model.set(GRB.IntParam.OutputFlag, outputMode);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public void initSPModel() throws GRBException {
        model.update();
//        minPhysicianConstrs = new GRBConstr[days*N];

        /**
         * parameters: s_n:n排班的时长，pi_n:排班对应的dual
         */
        double[] s_n = new  double[days*N];
        double[] pi_n = new  double[days*N];
        for(int j = 0; j< days*N; j++)
            s_n[j] = lengthOfShift; //根据实际设定调整
        for(int j = 0; j< days*N; j++)
            pi_n[j] = dualVals[j];

        // add variables
        x_n = new GRBVar[days*N];

        for(int j = 0; j< days*N; j++){
            x_n[j] = model.addVar(0,1,0,GRB.INTEGER, String.format("x_n[%d]", j));
        }

        // set objective
        GRBLinExpr obj = new GRBLinExpr();
        for(int j = 0; j< days*N; j++)
            obj.addTerm(s_n[j] - pi_n[j],   x_n[j]);
        model.setObjective(obj, GRB.MINIMIZE);

        // add constraints
        //每天1班约束
        for(int m = 1; m <= days; m++) {
            GRBLinExpr oneShiftConstr = new GRBLinExpr();
            for (int j = (m-1)* param.N; j <= m*param.N-1; j++)
                oneShiftConstr.addTerm(1.0, x_n[j]);
            model.addConstr(oneShiftConstr, GRB.LESS_EQUAL, 1,"oneShiftConstr(" + m + ")");
        }
        //夜班约束1
        for(int m = 1; m <= days-1; m++) {
            GRBLinExpr nightShiftConstr1 = new GRBLinExpr();
            nightShiftConstr1.addTerm(1.0, x_n[m*param.N-1]);
            for (int j = m*param.N; j <= (m+1)*param.N-1; j++)
                nightShiftConstr1.addTerm(1.0, x_n[j]);
            model.addConstr(nightShiftConstr1, GRB.LESS_EQUAL, 1,"oneShiftConstr1(" + m + ")");
        }
        //夜班约束2
        GRBLinExpr nightShiftConstr2 = new GRBLinExpr();
        nightShiftConstr2.addTerm(1.0, x_n[days*param.N-1]);
        for (int j = 0; j <= param.N-1; j++)
            nightShiftConstr2.addTerm(1.0, x_n[j]);
        model.addConstr(nightShiftConstr2, GRB.LESS_EQUAL, 1,"oneShiftConstr2");
        //夜班数量约束1
        GRBLinExpr maxNightShiftConstr = new GRBLinExpr();
        for(int m = 1; m <= days; m++)
            maxNightShiftConstr.addTerm(1.0, x_n[m*N-1]);
        model.addConstr(maxNightShiftConstr, GRB.LESS_EQUAL, C_max,"maxNightShiftConstr");

        //夜班数量约束2
        GRBLinExpr minNightShiftConstr = new GRBLinExpr();
        for(int m = 1; m <= days; m++)
            minNightShiftConstr.addTerm(1.0, x_n[m*N-1]);
        model.addConstr(minNightShiftConstr, GRB.GREATER_EQUAL, C_min,"minNightShiftConstr");
        //最大工作时间
        GRBLinExpr maxWorkingHourConstr = new GRBLinExpr();
        for(int n = 0; n < days*N; n++)
            maxWorkingHourConstr.addTerm(s_n[n], x_n[n]);
        model.addConstr(maxWorkingHourConstr, GRB.LESS_EQUAL, H,"maxWorkingHourConstr");

    }


    public CG_SP clone() {
        return new CG_SP(this);
    }

    public void setPrices(double[] dualVals, double dualVal) {
        this.dualVals = dualVals;
        this.dualVal = dualVal;

    }

    /**
     * 把必须包含和必须排除的时段反映到modDualVals
     *
     * @param mustInclude 必须包含的时段
     * @param mustExclude 必须排除的时段
     */
    private void setConstraints(final Set<Integer> mustInclude, final Set<Integer> mustExclude) {
        this.mustInclude = mustInclude;
        this.mustExclude = mustExclude;
    }

    public SolStatus solve(Set<Integer> mustInclude, Set<Integer> mustExclude) throws GRBException {
//        HashSet<Integer> result = new HashSet<>(mustExclude);
//        result.retainAll(mustInclude);
//        if (result.size() > 0) return SolStatus.INFEASIBLE;

//        setConstraints(mustInclude, mustExclude);
        return solveWithGurobi();
//        return checkSchedule();
    }

    // 可能会出现结果无法满足mustInclude和mustExclude的情况，这时候应当返回-1
    private SolStatus checkSchedule() {
        for (Integer t : mustExclude)
            if (schedule.get(t) == 1) return SolStatus.INFEASIBLE;

        for (Integer t : mustInclude)
            if (schedule.get(t) == 0) return SolStatus.INFEASIBLE;
        return SolStatus.OPTIMAL;
    }

    public static class label {
        //成员变量
        private double cost;
        private int[] path;
        private int node;
        private int C_work;
        private int C_night;
        private int omega;
//        private int dominant;

    }

    public static int compare(label l1, label l2) {
        if(l1.cost >= l2.cost){
            if(l1.C_work >= l2.C_work && l1.C_night >= l2.C_night){
                if((l1.omega <= 0 && l2.omega == 0))
                    return 1; // delete the left label l1
            }
        }
        else {
            if (l1.C_work <= l2.C_work && l1.C_night <= l2.C_night) {
                if ((l1.omega == 0 && l2.omega <= 0))
                    return -1; // delete the right label l2
            }
        }
        return 0; // non-dominant between l1 and l2

    }

    public static void copier(label l1, label l2) {
        l1.cost = l2.cost;
        l1.C_work = l2.C_work;
        l1.C_night = l2.C_night;
        l1.omega = l2.omega;
        l1.node = l2.node;
        for(int n = 0;n<7;n++)
            l2.path[n] = l2.path[n];

    }


    private double labelling(double[] dualVals) {
        //init
        int d =1;
        List<label> mylabel = new ArrayList<>();
        xn = new int[days];

        double[] s_n = new  double[days*N];
        double[] pi_n = new  double[days*N];
        for(int j = 0; j< days*N; j++)
            s_n[j] = lengthOfShift; //根据实际设定调整
        for(int j = 0; j< days*N; j++)
            pi_n[j] = dualVals[j];

        for(int i= 0; i < N + 1; i++){
            label labelOne = new label();
            labelOne.path = new int[7];
            if(i < N -1){
                labelOne.cost += s_n[i] - pi_n[i];
                labelOne.node = i;
                labelOne.path[d - 1] = labelOne.node;
                labelOne.C_work += s_n[i];
                labelOne.C_night += 0;
                labelOne.omega = 0;
                mylabel.add(labelOne);
            }
            if(i == N -1){
                labelOne.cost += s_n[i] - pi_n[i];
                labelOne.node = i;
                labelOne.path[d - 1] = labelOne.node;
                labelOne.C_work += s_n[i];
                labelOne.C_night += 1;
                labelOne.omega = -d;
                mylabel.add(labelOne);
            }
            if(i == N ) {
                labelOne.cost += 0;
                labelOne.node = -d;
                labelOne.path[d - 1] = labelOne.node;
                labelOne.C_work += 0;
                labelOne.C_night += 0;
                labelOne.omega = 0;
                mylabel.add(labelOne);
            }
        }

        //extending labels
        for(d=2;d <=7; d++){
            List<label> curlabel = mylabel.stream().collect(Collectors.toList());
            List<label> neolabel = new ArrayList<>();
            List<label> templabel = new ArrayList<>();
            for(int i= 0; i < N + 1; i++) {
                for(int k = 0, len = curlabel.size(); k < len; k++){
                    label newLable = new label();
                    newLable.path = new int[7];
                    newLable.cost = curlabel.get(k).cost;
                    for(int n = 0;n<7;n++)
                        newLable.path[n] = curlabel.get(k).path[n];
                    newLable.node = curlabel.get(k).node;
                    newLable.C_work = curlabel.get(k).C_work;
                    newLable.C_night = curlabel.get(k).C_night;
                    newLable.omega = curlabel.get(k).omega;

                    if(newLable.omega < 0){
                        newLable.cost += 0;
                        newLable.node = -d;
                        newLable.path[d - 1] = newLable.node;

                        newLable.C_work += 0;
                        newLable.C_night += 0;
                        newLable.omega = 0;
                        templabel.add(newLable);
                    }
                    else{
                        if(i < N -1){
                            if(d > 5){
                                if(newLable.C_work + lengthOfShift > H)
                                    continue;
                            }
                            newLable.cost += s_n[(d-1)*N+i] - pi_n[(d-1)*N+i];
                            newLable.node = (d-1)*N+i;
                            newLable.path[d - 1] = newLable.node;

                            newLable.C_work += s_n[(d-1)*N+i];
                            newLable.C_night += 0;
                            newLable.omega = 0;
                            templabel.add(newLable);
                        }
                        if(i == N -1){
                            if(d > 5){
                                if(newLable.C_work + lengthOfShift > H || newLable.C_night + 1 > C_max)
                                    continue;
                            }
                            newLable.cost += s_n[(d-1)*N+i] - pi_n[(d-1)*N+i];
                            newLable.node = (d-1)*N+i;
                            newLable.path[d - 1] = newLable.node;
                            newLable.C_work += s_n[(d-1)*N+i];
                            newLable.C_night += 1;
                            newLable.omega = -d;
                            templabel.add(newLable);
                        }
                        if(i == N ) {
                            newLable.cost += 0;
                            newLable.node = -d;
                            newLable.path[d - 1] = newLable.node;
                            newLable.C_work += 0;
                            newLable.C_night += 0;
                            newLable.omega = 0;
                            templabel.add(newLable);
                        }
//                            newLable = null;
                    }
                }
                //dominance rule
                List<label> copytemplabel = templabel.stream().collect(Collectors.toList());
                for(int k = 0, len = copytemplabel.size(); k< len - 1; k++){
                    for(int j = k+1; j < len; j++){

                        int dominance = compare(copytemplabel.get(k),copytemplabel.get(j));
                        if(dominance == 1){
                            copytemplabel.remove(k);
                            len--;
                            k--;
                            break;
                        }
                        if(dominance == -1){
                            copytemplabel.remove(j);
                            len--;
                            j--;
                            break;
                        }
                    }
                }

                List<label> copylabel = copytemplabel.stream().collect(Collectors.toList());
                templabel.clear();
                neolabel.addAll(copylabel);

            }
            List<label> copylabel = neolabel.stream().collect(Collectors.toList());
            neolabel.clear();
            mylabel = new ArrayList<>();
            mylabel = copylabel.stream().collect(Collectors.toList());
//                mylabel.addAll(newlabel);

        }

        //
        double res = 1e10;
        label labelOpt = new label();
        labelOpt.path = new int[7];
        for(int k = 0, len = mylabel.size(); k< len; k++){
            if(mylabel.get(k).cost < res){
                labelOpt.cost = mylabel.get(k).cost;
                res=labelOpt.cost;
                labelOpt.path = mylabel.get(k).path;
            }
        }

        for(int k = 0; k< 7; k++){
            xn[k] = labelOpt.path[k];
            //System.out.print(labelOpt.path[k]+"\n");
        }


        return res;



    }

    public SolStatus solveWithlabelling() throws GRBException {
        xn = new int[days];
        objVal = labelling(dualVals) - dualVal;

        // 输出对应的schedule结果到schedule和scheduleLen
        int scheduleLength = 0;
        schedule = new ArrayList<>(days*N);
        for (int j = 0; j < days*N; j++) schedule.add(0.0);
        int negativeCount = 0;
        for(int k = 0; k< days; k++){
            if(xn[k] < 0){
                negativeCount++;
                continue;
            }
            else{
                schedule.set(xn[k], 1.0);
                scheduleLength += lengthOfShift; // add the length of shift j
            }
        }
        scheduleLen = scheduleLength;

        if (negativeCount == days || objVal > -err) {
            return SolStatus.INFEASIBLE;
        }
        else
            return SolStatus.OPTIMAL;

    }

    public SolStatus solveWithGurobi() throws GRBException {
        model.optimize();

        objVal = model.get(GRB.DoubleAttr.ObjVal) - dualVal;
        /*
        System.out.print("x_n, CG-SP solve with Gurobi");
        for (int n = 0; n < days * N; n++)
            System.out.print("\n"+n+" "+x_n[n].get(GRB.DoubleAttr.X));

        System.out.print("pi_n, CG-SP solve with Gurobi");
        for (int n = 0; n < days * N; n++)
            System.out.print("\n"+n+" "+dualVals[n]);

        try{
            Thread.sleep(10000);
        }catch(Exception e){
        }
        */




        // 输出对应的schedule结果到schedule和scheduleLen
        int scheduleLength = 0;

        schedule = new ArrayList<>(days*N);
        for (int j = 0; j < days*N; j++) schedule.add(0.0);
        for (int j = 0; j < days*N; j++) {
            if (x_n[j].get(GRB.DoubleAttr.X) > 1 - err && x_n[j].get(GRB.DoubleAttr.X) < 1 + err ){
                schedule.set(j, 1.0);
                scheduleLength += lengthOfShift; // add the length of shift j
            }
        }
        scheduleLen = scheduleLength;



        if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
            return SolStatus.INFEASIBLE;
        }
        else
            return SolStatus.OPTIMAL;
    }




    public void clear() {
        objMemo = new HashMap<>();
        solMemo = new HashMap<>();
    }
}

