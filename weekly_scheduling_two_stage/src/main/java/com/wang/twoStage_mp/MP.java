package com.wang.twoStage_mp;

import com.wang.enums.LocalSearchSolStatus;
import com.wang.twoStage_mp.util.BPSFFA;
import com.wang.enums.SolStatus;

import gurobi.*;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

import static com.wang.param.*;

/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang.twoStage_mp
 * Version: 1.0
 */
@Slf4j
@Data
public class MP {
    private int shifts[][];
    private GRBModel model;
    private GRBVar[] Ls1;          // GRB变量：每时段队长
    private GRBVar[] Ls2;          // GRB变量：每时段队长
    private GRBVar[] ct;          // GRB变量：每时段医生数
    private GRBVar[] x_n;         // GRB变量：每排班医生数

    private GRBVar[][] p_tk;     // GRB变量：p_tk,医生数标识变量
    private GRBVar[][][] r_tkv;  // GRB变量：r_tkv,rho1标识变量
    private GRBVar[][] r2_tv;    // GRB变量：r2_tv,rho2标识变量

    private GRBVar[] twd;       // GRB变量：每天的总工作时长
    private double[] twdVal;    // 求解结果：每天的总工作时长

    private double[][] range;   // rho 的上界
    private double[][] slope;   // 分段线性的斜率

    private double objVal = 0;  // 目标值
    private double[] p;         // 不同时段医生数量

    private int genCutCount;    //生成的cut计数
    private GRBGenConstr[] latestGenConstrs;   //最近一次添加的Gen constrs

    private int mpStatus;    // 当前mp是整数(1)还是连续(0)
    private Set<String> addedCuts;   // 已经添加的cut，防止邻域搜索重复添加
    private Map<String, LocalSearchSolStatus> searchedCuts;  //已经搜索过的cut，防止邻域搜索重复评估

    public MP(GRBEnv env) {
        try {
            env.set(GRB.DoubleParam.MIPGap, 0.03);
            model = new GRBModel(env);
            model.set(GRB.IntParam.OutputFlag, outputMode);
            addedCuts = new HashSet<>();
            searchedCuts = new HashMap<>();
            p = new double[days*N];
//            for (int n = 0; n < N*days; n++)
//                p[n] = 0;

//            readParams("data/separation_p5.txt");

            splineReg();
            initModel();
            mpStatus = 1;
        }
//        catch (GRBException | IOException e) {
        catch (GRBException e) {

            e.printStackTrace();
        }
    }

    /**
     * 读取线性化参数
     *
     * param filename 参数文件
     */
    /*
    public void readParams(String filename) throws IOException {
        File file = new File(filename);
        range = new double[params.N][params.PARTITIONS];
        slope = new double[params.N][params.PARTITIONS];
        BufferedReader br = new BufferedReader(new FileReader(file));
        for (int i = 0; i < params.N; i++) {
            for (int j = 0; j < params.PARTITIONS; j++) {
                String[] tmp = br.readLine().split(" ");
                range[i][j] = Double.parseDouble(tmp[0]);
                slope[i][j] = Double.parseDouble(tmp[1]) * 10000;
            }
            br.readLine();
        }
    }
    */

    public void splineReg(){
        int num = Math.max(Num, c_aux)+1;

        range = new double[V][num];
        slope = new double[V][num];
        for (int i = 0; i < V; i++)
            for (int n = 0; n < num; n++){
                range[i][n] = 0;
                slope[i][n] = 0;
            }
        for (int s = 1; s < num; s++){
            double L_now = 0;
            double rho_now = 0;
            double L_target = 0;
            double rho_target = 0;
            for (int i = 0; i < V; i++){
                L_target = (i + 1.0) / V * Limit;
                rho_target = BPSFFA.findRange(s,L_target);
                range[i][s] = rho_target - rho_now;
                slope[i][s] = (L_target - L_now) / range[i][s];
                rho_now = rho_target;
                L_now = L_target;
            }
        }
    }


    /**
     * 初始化主问题模型，变量+约束
     * name format:
     * var: x[i]
     * constr: c(i)
     */
    private void initModel() {
        genAllShifts();
        try {
            // add vars
            Ls1 = new GRBVar[T+1];
            Ls2 = new GRBVar[T+1];
            ct = new GRBVar[T];
            x_n = new GRBVar[N*days];
            p_tk = new GRBVar[T][Num+1];
            r_tkv = new GRBVar[T+1][Num+1][V];
            r2_tv = new GRBVar[T+1][V];

            twd = new GRBVar[days];

            for (int d = 0; d < days; d++) {
                twd[d] = model.addVar(hours, T, 1, GRB.INTEGER, String.format("twd[%d]", d + 1));
            }

            for (int t = 0; t < T+1; t++){
                Ls1[t] = model.addVar(0,Limit,0,GRB.CONTINUOUS,String.format("Ls1[%d]", t));
                Ls2[t] = model.addVar(0,Limit,0,GRB.CONTINUOUS,String.format("Ls2[%d]", t));
            }

            for (int t = 0; t < T; t++)
                ct[t] = model.addVar(1,Num,0,GRB.INTEGER,String.format("ct[%d]", t));

            for (int n = 0; n < N*days; n++)
                x_n[n] = model.addVar(0,1,0,GRB.BINARY,String.format("x_n[%d]", n));

            for (int t = 0; t < T; t++)
                for (int k = 0; k < Num+1; k++)
                    p_tk[t][k] = model.addVar(0,1,0,GRB.BINARY,String.format("p_tk[%d,%d]", t+1,k));

            for (int t = 0; t < T+1; t++)
                for (int k = 0; k < Num+1; k++)
                    for (int v = 0; v < V; v++)
                        r_tkv[t][k][v] = model.addVar(0,V,0,GRB.CONTINUOUS,String.format("r_tkv[%d,%d,%d]", t,k,v));

            for (int t = 0; t < T+1; t++)
                for (int v = 0; v < V; v++)
                    r2_tv[t][v] = model.addVar(0,V,0,GRB.CONTINUOUS,String.format("r2_tv[%d,%d]", t,v));

            // add constraints
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);  //目标函数在twd变量中已设置

            //acceleration strategies
//            model.set(GRB.IntAttr.NumStart, 2);
//            for (int n = 0; n < days * N; n++) {
//                x_n[n].set(GRB.DoubleAttr.Start, warmStart[0][n]);
//                x_n[n].set(GRB.DoubleAttr.Start, warmStart[1][n]);
//
//                if(Math.abs(Math.round(warmStart[0][n])-warmStart[0][n]) <= 0.02){
//                    if(Math.abs(Math.round(warmStart[1][n])-warmStart[1][n]) <= 0.02){
//                        if(Math.round(warmStart[0][n]) == Math.round(warmStart[1][n])){
//                            GRBLinExpr expr = new GRBLinExpr();
//                            expr.addTerm(1, x_n[n]);
//                            model.addConstr(expr, GRB.EQUAL, Math.round(warmStart[1][n]), String.format("cuts", n + 1));
//                        }
//                    }
//                }
//            }

            //twd(每天总工作时长)计算方法
            for (int d = 0; d < days; d++) {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1, twd[d]);
                for (int t = 0; t < T; t++) {
                    if(t / hours == d)
                        expr.addTerm(-1, ct[t]);
                }
                model.addConstr(expr, GRB.GREATER_EQUAL, 0, String.format("twd(%d)", d + 1));
            }


            // ct和排班的关系绑定
            for (int t = 0; t < T; t++) {
                GRBLinExpr bindShift = new GRBLinExpr();
                for (int n = 0; n < N*days; n++) {
                        bindShift.addTerm(shifts[n][t], x_n[n] );
                }
                model.addConstr(bindShift, GRB.EQUAL, ct[t], "bindShift(" + (t + 1) + ")");
            }

            //最大队长约束
            for (int t = 0; t < T; t++) {
                GRBLinExpr maxQL = new GRBLinExpr();
                maxQL.addTerm(1.0, Ls1[t]);
                model.addConstr(maxQL, GRB.LESS_EQUAL, LsLimit, "maxQL(" + (t + 1) + ")");
            }

            //p_tk约束1
            for (int t = 0; t < T; t++) {
                GRBLinExpr ptk1 = new GRBLinExpr();
                for (int k = 1; k <= Num; k++)
                    ptk1.addTerm(1.0, p_tk[t][k]);
                model.addConstr(ptk1, GRB.EQUAL, 1, "ptk1(" + (t + 1) + ")");
            }

            //p_tk约束2
            for (int t = 0; t < T; t++) {
                GRBLinExpr ptk2 = new GRBLinExpr();
                for (int k = 1; k <= Num; k++)
                    ptk2.addTerm(k, p_tk[t][k]);
                model.addConstr(ptk2, GRB.EQUAL, ct[t], "ptk2(" + (t + 1) + ")");
            }

            //r_tkv约束
            for (int t = 0; t < T; t++)
                for (int k = 1; k <= Num; k++)
                    for (int v = 0; v < V; v++){
                        GRBLinExpr rtkv1 = new GRBLinExpr();
                        rtkv1.addTerm(range[v][k], p_tk[t][k]);
                        model.addConstr(rtkv1, GRB.GREATER_EQUAL, r_tkv[t+1][k][v], "r_tkv(" + (t + 1) + ")");
                    }

            //r2_tv约束
            for (int t = 0; t < T; t++)
                for (int v = 0; v < V; v++){
                    GRBLinExpr rtkv2 = new GRBLinExpr();
                    rtkv2.addTerm(1.0, r2_tv[t+1][v]);
                    model.addConstr(rtkv2, GRB.LESS_EQUAL, range[v][c_aux], "r2_tv(" + (t + 1) + ")");
                }


            //Ls1计算
            for (int t = 0; t < T; t++){
                GRBLinExpr Ls1Cosntr = new GRBLinExpr();
                for (int k = 1; k <= Num; k++)
                    for (int v = 0; v < V; v++)
                        Ls1Cosntr.addTerm(slope[v][k], r_tkv[t+1][k][v]);
                model.addConstr(Ls1Cosntr, GRB.EQUAL, Ls1[t+1], "Ls1(" + (t + 1) + ")");
            }


            //Ls2计算
            for (int t = 0; t < T; t++){
                GRBLinExpr Ls2Constr = new GRBLinExpr();
                for (int v = 0; v < V; v++)
                    Ls2Constr.addTerm(slope[v][c_aux], r2_tv[t+1][v]);
                model.addConstr(Ls2Constr, GRB.EQUAL, Ls2[t+1], "Ls2(" + (t + 1) + ")");

            }


            //流平衡1
            for (int t = 0; t < T; t++){
                GRBLinExpr Banlance1 = new GRBLinExpr();

                Banlance1.addTerm(-1.0, Ls1[t]);
                Banlance1.addTerm(1.0, Ls1[t+1]);

                for (int v = 0; v < V; v++)
                    Banlance1.addTerm(-1.0*c_aux*mu_aux*delta, r2_tv[t+1][v]);

                for (int k = 1; k <= Num; k++)
                    for (int v = 0; v < V; v++)
                        Banlance1.addTerm(k*mu*delta, r_tkv[t+1][k][v]);

                model.addConstr(Banlance1, GRB.EQUAL, lamda[t], "Ls1(" + (t + 1) + ")");
            }

            //流平衡2
            for (int t = 0; t < T; t++){
                GRBLinExpr Banlance2 = new GRBLinExpr();

                Banlance2.addTerm(1.0, Ls2[t]);

                for (int v = 0; v < V; v++)
                    Banlance2.addTerm(-1.0*c_aux*mu_aux*delta, r2_tv[t+1][v]);

                for (int k = 1; k <= Num; k++)
                    for (int v = 0; v < V; v++)
                        Banlance2.addTerm(k*mu*delta*prob, r_tkv[t+1][k][v]);

                model.addConstr(Banlance2, GRB.EQUAL, Ls2[t+1], "Ls2(" + (t + 1) + ")");

            }

            //InitialCondition1
            GRBLinExpr Ls1Ini = new GRBLinExpr();
            Ls1Ini.addTerm(1.0, Ls1[0]);
            model.addConstr(Ls1Ini, GRB.EQUAL, 0, "Ls1[0]]");

            //InitialCondition2
            GRBLinExpr Ls2Ini = new GRBLinExpr();
            Ls2Ini.addTerm(1.0, Ls2[0]);
            model.addConstr(Ls2Ini, GRB.EQUAL, 0, "Ls2[0]]");


        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void genAllShifts(){
        shifts = new int[days * N][T];
        // 8 shifts
        for (int n = 0; n < days * N; n++) {
            for (int t = 0; t < T; t++) {
                shifts[n][t] = 0;
            }
        }

        for (int m = 0; m < days; m++) {
            for (int n = m * N; n < (m + 1) * N; n++) {
                if (n % N == 0) {
                    for (int t = 0 + 24 * m; t < 8 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 1) {
                    for (int t = 1 + 24 * m; t < 9 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 2) {
                    for (int t = 2 + 24 * m; t < 10 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 3) {
                    for (int t = 3 + 24 * m; t < 11 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 4) {
                    for (int t = 4 + 24 * m; t < 12 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 5) {
                    for (int t = 6 + 24 * m; t < 14 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 6) {
                    for (int t = 8 + 24 * m; t < 16 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

                if (n % N == 7) {
                    for (int t = 16 + 24 * m; t < 24 + 24 * m; t++) {
                        shifts[n][t] = 1;
                    }
                }

            }
        }
    }

//    private void genAllShifts() {
//        // {n,t}:{第几个排班，时段}
//        shifts = new ArrayList<>();
//        // 空班次：即不用该医生
//        shifts.add(new int[]{0, 0});
//        // 夜班
//        shifts.add(new int[]{8, 23});
//        for (int n = params.S_LB; n <= params.S_UB; n++)
//            for (int t = n - 1; t < params.T - 8; t++)
//                shifts.add(new int[]{n, t});
//    }


    public void saveResult() throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter("data/result.txt", true));
        br.write("*****************  MP RESULT  *****************\n");
        br.write("obj: " + objVal + "\n");
        br.write("p[t]: \n");
        for (int n = 0; n < days * N; n++) {
            br.write("\n" + p[n]);
//            br.write('\n');
        }
        br.write("\n");
        br.close();
    }

    public SolStatus solve() {
        try {
            model.optimize();
            if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                log.warn("Infeasible benders MP!");
                return SolStatus.INFEASIBLE;
            }

//            for (int n = 0; n < days * N; n++)
//                System.out.print("\n"+n+" "+x_n[n].get(GRB.DoubleAttr.X));


            twdVal = model.get(GRB.DoubleAttr.X, twd);

        } catch (GRBException e) {
            e.printStackTrace();
        }
        return SolStatus.OPTIMAL;
    }

    /**
     * 取最优值
     *
     * @return 主问题目标值：医生数量
     * @throws GRBException
     */
    public double getObj() throws GRBException {
        objVal = model.get(GRB.DoubleAttr.ObjVal);
        return objVal;
    }

    public double[] getSol() throws GRBException {
        for (int n = 0; n < N*days; n++){
            p[n] = model.getVarByName("x_n[" + n + "]").get(GRB.DoubleAttr.X);
        }

//        System.out.print("p_t");
//        for (int n = 0; n < N*days; n++)
//            System.out.print("\n"+n+" "+p[n]);
//        for (int t = 0; t < T; t++)
//            p[t] = model.getVarByName("ct[" + t + "]").get(GRB.DoubleAttr.X);

        return p;
    }



}
