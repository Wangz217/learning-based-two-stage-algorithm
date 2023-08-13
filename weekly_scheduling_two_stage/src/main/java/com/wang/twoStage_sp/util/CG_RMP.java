package com.wang.twoStage_sp.util;

import com.wang.param;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.wang.enums.SolStatus;
import gurobi.*;

import java.util.*;

import static com.wang.param.*;


/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang.twoStage_sp.util
 * Version: 1.0
 */
@Slf4j
@Data
public class CG_RMP {
    private Map<String, Integer> allScheduleSet;    // 所有排班的集合,value是对应在schedules中的索引 key=encoded schedules
    private List<List<Double>> schedules;            // 保存的排班，记录各排班对应的上班班次
    private List<Integer> scheduleLengths;            // 记录schedules对应的排班时长

    private double objVal;                        //rmp 的目标值
    private double[] p;                            // 从主问题传过来的期望医生数量
    private boolean isIntSol = true;                // 是否整数解
    private double[] twd;                        // 每周的总工作时长

    // gurobi related fields
    private GRBEnv env;
    private GRBModel model;
    private List<GRBVar> y;               // 变量
    private GRBVar[] theta_j;                  //变量
    private double[] d_j;
    private double[][] a_nj;

    private GRBConstr[] minPhysicianConstrs;  // 最小医生数量约束
    private GRBConstr maxPhysicianConstrs;  // 最大医生数量约束

    private double[] minPhysicianDuals;       // minPhysician约束对偶变量
    private double maxPhysicianDuals;       // minPhysician约束对偶变量

    /**
     * 克隆本模型
     *
     * @return 新模型
     */
    public CG_RMP clone() {
        return new CG_RMP(this);
    }

    /**
     * 从另一个RMP初始化本模型
     *
     * @param other 另一个RMP
     */
    public CG_RMP(CG_RMP other) {
        this.env = other.env;
        this.schedules = new ArrayList<>(other.schedules);
        this.scheduleLengths = new ArrayList<>(other.scheduleLengths);
        this.allScheduleSet = new HashMap<>(other.allScheduleSet);

        this.p = other.p;
        this.minPhysicianDuals = new double[days*N];

        try {
            this.model = new GRBModel(this.env);
            this.model.set(GRB.IntParam.OutputFlag, outputMode);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public CG_RMP(GRBEnv env) {
        this.env = env;
        allScheduleSet = new HashMap<>();

        schedules = new ArrayList<>();
        scheduleLengths = new ArrayList<>();
        p = new double[days*N];
        minPhysicianDuals = new double[days*N];

        try {
            model = new GRBModel(env);
            model.set(GRB.IntParam.OutputFlag, outputMode);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    // 是否整数解
    public boolean isIntegerSol() {
        return isIntSol;
    }

    /**
     * 更新每时段医生数量下限
     *
     * @param p_t 新的医生数量下限数组
     */
    public void resetP(double[] p_t) {
        p = p_t;
        try {
            model.update();
            for (int n = 0; n < days*N; n++) {
                minPhysicianConstrs[n].set(GRB.DoubleAttr.RHS, p[n]);

            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Description: 给每个医生加一个超级班次
     */
    public void addInitShifts() {
        List<Double> initShifts = new ArrayList<>();
        for (int n = 0; n < days*N; n++)
            initShifts.add(0.3);
        for (int j = 0; j < Num; j++)
            schedules.add(j,initShifts);

        for (int j = 0; j < Num; j++)
            scheduleLengths.add(days*N*lengthOfShift); //超级列的服务时长
    }

    /**
     * Description: 初始MP
     */
    public void initModel() throws GRBException {
        model.update();
        minPhysicianConstrs = new GRBConstr[days*N];




        // add variables
//        theta_j = new GRBVar[Num];

        /**
         * parameters: d_j:对应排班的时长，a_nj:对应排班的具体解
         */
//        d_j = new  double[schedules.size()];
//        a_nj = new  double[schedules.size()][days*N];
//
//        for(int j = 0; j< schedules.size(); j++) {
//            d_j[j] = scheduleLengths.get(j);
//            for(int n = 0; n< days*N; n++) {
//                a_nj[j][n] = schedules.get(j).get(n);
//            }
//        }

        // add variables
        y =  new ArrayList<>();
        for(int j = 0; j< schedules.size(); j++){
//            theta_j[j] = model.addVar(0,1,0,GRB.INTEGER, String.format("theta_j[%d]", j+1));
            y.add(model.addVar(0,1,scheduleLengths.get(j),GRB.CONTINUOUS, String.format("y[%d]", j+1)));
        }

        // set objective
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

        // add constraints
        //最小排班数量约束
        for(int n = 0; n < days * N; n++) {
            GRBLinExpr minPhysicianNum = new GRBLinExpr();
            for (int j = 0; j < y.size(); j++) {
                minPhysicianNum.addTerm(schedules.get(j).get(n), y.get(j));
            }
            minPhysicianConstrs[n] = model.addConstr(minPhysicianNum, GRB.GREATER_EQUAL, p[n],"xnConstr" + (n+1) + ")");

        }

        //最大总排班约束
        GRBLinExpr sumConstr = new GRBLinExpr();
        for (int j = 0; j < y.size(); j++) {
            sumConstr.addTerm(1.0, y.get(j));
        }
        maxPhysicianConstrs = model.addConstr(sumConstr, GRB.LESS_EQUAL, Num,"sumConstr");

    }


    // 添加列到排班集合中
    public void addCol(List<Double> schedule, int scheduleLength) throws GRBException {

//      allScheduleSet.put(String.valueOf(j), schedules.size());
        schedules.add(new ArrayList<>(schedule));
        scheduleLengths.add(scheduleLength);

        double[] coef = new double[days*N];   // 班次系数，对应模型a_nj
        for (int t = 0; t < days*N; t++)
            coef[t] = 1.0 * schedule.get(t);

        model.update();
        GRBColumn col = new GRBColumn();
        col.addTerms(coef, minPhysicianConstrs);
        col.addTerm(1.0, maxPhysicianConstrs);
        y.add(model.addVar(0, 1, scheduleLength, GRB.CONTINUOUS, col, "y[" + (schedules.size() + 1) + "]"));
    }


    public SolStatus solve() throws GRBException {
        log.debug("value of schedule.size: {}", schedules.size());
        model.optimize();
        if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
            return SolStatus.INFEASIBLE;
        }
        objVal = model.get(GRB.DoubleAttr.ObjVal);


        // 确定是否整数解
        for (int m = 0; m < schedules.size(); m++)
            log.debug("value of theta_j[" + m + "]: {}", y.get(m).get(GRB.DoubleAttr.X));
        isIntSol = true;
        for (int n = 0; n < schedules.size(); n++) {
            double v = y.get(n).get(GRB.DoubleAttr.X);
            double vRounded = Math.round(v);
            if (Math.abs(v - vRounded) >= param.err) {
                isIntSol = false;
                break;
            }
        }

        //返回所需的dual系数pi
        for (int n = 0; n < days*N; n++) {
            minPhysicianDuals[n] = minPhysicianConstrs[n].get(GRB.DoubleAttr.Pi);
        }
        maxPhysicianDuals = maxPhysicianConstrs.get(GRB.DoubleAttr.Pi);




        return SolStatus.OPTIMAL;
    }

    public double[] getMinPhysicianDuals() {
        return minPhysicianDuals;
    }



    // 返回分支点j
    public int findBranchPoint() throws GRBException {
        int[] branchingPoint = new int[2];
        double minDistToZero = 0.5;  // theat_j与0的最小距离
        double minDistToOne = 0.5;  // theat_j与1的最小距离
        branchingPoint[0] = 0;  //记录与0最近的分支点
        branchingPoint[1] = 0;  //记录与1最近的分支点
        for (int j = 0; j < schedules.size(); j++) {
            boolean foundInT = false;
            double Distance = 1;
            double Val = 0;
            Val =  y.get(j).get(GRB.DoubleAttr.X);

            //与0的距离
            if (Val < err && Val > - err) {
                continue;
            }
            else {
                Distance = Math.abs(Val - 0);
                if (Distance < minDistToZero){
                    minDistToZero = Distance;
                    branchingPoint[0] = j;
                }

            }

            //与1的距离
//            if (Val > 1 - err && Val < 1 + err) {
//                continue;
//            }
//            else {
//                Distance = Math.abs(Val - 1);
//                if (Distance < minDistToOne){
//                    minDistToOne = Distance;
//                    branchingPoint[1] = j;
//                }
//            }

        }
//        if (minDistToOne > minDistToZero){
//            log.debug("branching point: ", branchingPoint[1]);
//            log.debug("min distance to One: {}", minDistToOne);
//            return branchingPoint[1];
//
//        }
//        else{
//            log.debug("branching point: ", branchingPoint[0]);
//            log.debug("min distance to Zero: {}", minDistToZero);
//            return branchingPoint[0];
//        }
            log.debug("branching point: {}", branchingPoint[0]);
            log.debug("min distance to Zero: {}", minDistToZero);
            return branchingPoint[0];

    }

    public List<List<Double>> getSol() throws GRBException {
        List<List<Double>> sol = new ArrayList<>();

        int roster = -1;
        for (int j = 0; j < y.size(); j++) {
            if (Math.abs(y.get(j).get(GRB.DoubleAttr.X) - 1) < param.err) {
                roster = j;
                break;
            }
        }
        sol.add(schedules.get(roster));

        return sol;
    }

    // 根据分支点，设置constraint
    public void resetCosntraint(int branchingPoint, int choice) throws GRBException {
//        for(int i = 0; i < schedules.size();i ++){
//            List<Double> indices = schedules.get(i);
            if (choice == 0) { //branchingPoint处取0
                model.update();
                GRBLinExpr branchConstr = new GRBLinExpr();
                branchConstr.addTerm(1.0, y.get(branchingPoint));
                model.addConstr(branchConstr, GRB.EQUAL, 0.0,"branchConstrs" + (choice));
            } else { //branchingPoint处取1
                model.update();
                GRBLinExpr branchConstr = new GRBLinExpr();
                branchConstr.addTerm(1.0, y.get(branchingPoint));
                model.addConstr(branchConstr, GRB.EQUAL, 1.0,"branchConstrs" + (choice));

            }
//        }

    }


}
