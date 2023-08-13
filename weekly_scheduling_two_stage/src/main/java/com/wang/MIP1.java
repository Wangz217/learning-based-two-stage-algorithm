package com.wang;

import gurobi.*;
import com.wang.param;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import static com.wang.param.*;


/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang
 * Version: 1.0
 */
@Slf4j
@Data
public class MIP1 {

    private GRBModel model;
    private GRBVar[] twd;          // GRB变量：每天的总工作时长
    private double[] twdVal;    // 求解结果：每天的总工作时长
    private GRBVar[][] p_dt;       // GRB变量：医生数量
    private GRBVar[][] r;          // GRB变量：选择block
    private GRBVar[][][] p_dtk;    // GRB变量：p_dt的标识变量
    private double[][] range;   // rho 的上界
    private double[][] slope;   // 分段线性的斜率
    private GRBVar[] xn;


    public void MIP(String[] args) {
        try {
            GRBEnv env = new GRBEnv("mip1.log");
            env.set(GRB.DoubleParam.MIPGap, 0.01);
            model = new GRBModel(env);
            model.set(GRB.IntParam.OutputFlag, 0);

//            readParams("data/separation_p5.txt");
            initModel();

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

//    public void readParams(String filename) throws IOException {
//        File file = new File(filename);
//        range = new double[params.N][params.PARTITIONS];
//        slope = new double[params.N][params.PARTITIONS];
//        BufferedReader br = new BufferedReader(new FileReader(file));
//        for (int i = 0; i < params.N; i++) {
//            for (int j = 0; j < params.PARTITIONS; j++) {
//                String[] tmp = br.readLine().split(" ");
//                range[i][j] = Double.parseDouble(tmp[0]);
//                slope[i][j] = Double.parseDouble(tmp[1]) * 10000;
//            }
//            br.readLine();
//        }
//    }

    //SP model temporary
    private void initModel() {
        try {

        double[] sn;
        double[] pin;
        sn = new double[Num];
        pin = new double[Num];
        xn = new GRBVar[days*N];

        // add variables
        for(int n = 0; n < days * N; n++)
            xn[n] = model.addVar(0,1,0,GRB.INTEGER, String.format("a_nj[%d]", n+1));


        // set objective
        GRBLinExpr obj = new GRBLinExpr();
        for(int j = 0; j< Num; j++)
            obj.addTerm((sn[j]-pin[j]), xn[j]);
        model.setObjective(obj, GRB.MINIMIZE);

        // add constraints
        //每天1班约束
        for(int m = 1; m <= days; m++) {
            GRBLinExpr oneShiftConstr = new GRBLinExpr();
            for (int j = (m-1)*param.N; j <= m*param.N-1; j++)
                oneShiftConstr.addTerm(1.0, xn[j]);
            model.addConstr(oneShiftConstr, GRB.LESS_EQUAL, 1,"oneShiftConstr(" + m + ")");
        }
        //夜班约束1
        for(int m = 1; m <= days-1; m++) {
            GRBLinExpr nightShiftConstr1 = new GRBLinExpr();
            nightShiftConstr1.addTerm(1.0, xn[m*param.N-1]);
            for (int j = m*param.N; j <= (m+1)*param.N-1; j++)
                nightShiftConstr1.addTerm(1.0, xn[j]);
            model.addConstr(nightShiftConstr1, GRB.LESS_EQUAL, 1,"oneShiftConstr1(" + m + ")");
        }
        //夜班约束2
        GRBLinExpr nightShiftConstr2 = new GRBLinExpr();
        nightShiftConstr2.addTerm(1.0, xn[days*param.N-1]);
        for (int j = 0; j <= param.N-1; j++)
            nightShiftConstr2.addTerm(1.0, xn[j]);
        model.addConstr(nightShiftConstr2, GRB.LESS_EQUAL, 1,"oneShiftConstr2");

        GRBLinExpr maxNightShiftConstr = new GRBLinExpr();
        for(int m = 1; m <= days; m++)
            maxNightShiftConstr.addTerm(1.0, xn[m*N-1]);
        model.addConstr(maxNightShiftConstr, GRB.LESS_EQUAL, C_max,"maxNightShiftConstr");


        GRBLinExpr minNightShiftConstr = new GRBLinExpr();
        for(int m = 1; m <= days; m++)
            minNightShiftConstr.addTerm(1.0, xn[m*N-1]);
        model.addConstr(minNightShiftConstr, GRB.GREATER_EQUAL, C_min,"minNightShiftConstr");

        GRBLinExpr maxWorkingHourConstr = new GRBLinExpr();
        for(int n = 0; n < days*N; n++)
            maxWorkingHourConstr.addTerm(sn[n], xn[n]);
        model.addConstr(maxWorkingHourConstr, GRB.LESS_EQUAL, H,"maxWorkingHourConstr");

        } catch (GRBException e) {
            e.printStackTrace();
        }


    }
}
