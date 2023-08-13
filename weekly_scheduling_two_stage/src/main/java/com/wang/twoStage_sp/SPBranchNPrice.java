package com.wang.twoStage_sp;

import com.wang.param;
import com.wang.twoStage_sp.util.BBNode;
import com.wang.enums.SolStatus;

import gurobi.GRBEnv;
import gurobi.GRBException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;



/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang.twoStage_sp
 * Version: 1.0
 */
@Slf4j
@Data
public class SPBranchNPrice {
    private double objVal;      //SP最优值
    private List<List<Double>> incumbent;
    private Stack<BBNode> nodes;
    private BBNode root;
    private int nodeCount;
    private int[] solStaffing;  // 子问题求解得到的staffing
    private double[] twd;       // 每天的总工作时长

    public SPBranchNPrice(GRBEnv env) throws GRBException {
        nodes = new Stack<>();
        nodeCount = 1;
        root = new BBNode(env, nodeCount);
    }

    public double getObj() {
        return objVal;
    }



    /**
     * 求解分支定价
     *
     * @return 求解状态
     * @throws GRBException
     */
    public SolStatus solve(double[] p_t) throws GRBException {
        resetStaffing(p_t);
        boolean feasible = false;
        while (!nodes.empty()) {
            BBNode node = nodes.pop();
            log.info("============== solving node {} ==============", node.getID());
            if (node.solve() == SolStatus.OPTIMAL) {  // status = 1 --> 最优解
                feasible = true;
                if (node.getRmp().isIntegerSol()) {
                    //RMP最优解是整数
                    if (node.getObj() < objVal - param.err) {
                        log.info("Found integer sol, current node obj val: {}", node.getObj());
                        objVal = node.getObj();
                        twd = node.getTWD();
                        incumbent = node.getSol();
                    }
                } else if (node.getObj() < objVal - param.err) {
                    log.info("non-integer sol, current node obj val: {}", node.getObj());
                    log.info("current obj val: {}", objVal);

                    branch(node);
                }
            } else {
                //RMP无解
                log.warn("Infeasible BP node!");
                continue;
            }
        }
        if (feasible) {
            extractStaffing();
            log.info("mo more improvement");
            return SolStatus.OPTIMAL;
        }
        return SolStatus.INFEASIBLE;
    }

    private void extractStaffing() {
        solStaffing = new int[param.days * param.N];
        for (int n = 0; n < param.days * param.N; n++) {
            for (int i = 0; i < incumbent.size(); i++) {
                if (incumbent.get(i).get(n) == 1)
                    solStaffing[n]++;
            }
        }
    }

    public double getCutInfo() {
        return objVal;
    }

    public void saveResult() throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter("data/result.txt", true));  // true for append
        br.write("\n*****************  SP RESULT  *****************\n");
        br.write("obj: " + objVal + "\n");
        br.write("p[t]: ");
        for (int n = 0; n < param.days * param.N; n++) {
            br.write(String.valueOf(solStaffing[n]) + ", ");
        }
        br.write("\n");

        br.write("x[i][t]: \n");
        for (int n = 0; n < param.days * param.N; n++) {
            for (int i = 0; i < incumbent.size(); i++) {
                br.write(String.valueOf(incumbent.get(i).get(n)) + ", ");
            }
            br.write("\n");
        }

        br.write("\n\n");
        br.close();
    }

    private void resetStaffing(double[] p_t) {
        objVal = Integer.MAX_VALUE;
        root.resetP(p_t);
        nodes.add(root);
        log.debug("min number of physicians: {}, ", p_t);
    }

    public int[] getStaffing() {
        return solStaffing;
    }

    // 分支
    public void branch(BBNode node) throws GRBException {
        log.debug("branching at node: {}, get children {} and {}", node.getID(), nodeCount + 1, nodeCount + 2);
        int branchingPoint = node.findBranchPoint();
        BBNode leftNode = node.clone(nodeCount + 1);
        node = node.clone(nodeCount + 2);

        /*
        //左节点不包含该排班theta
        leftNode.getMustExclude().get(leftNode.getID()).add(branchingPoint);
        leftNode.getRmp().initModel();
        //节点应包含的所有约束
        for(int i = 0; i < leftNode.getMustExclude().get(leftNode.getID()).size(); i++)
            leftNode.getRmp().resetCosntraint(leftNode.getMustExclude().get(leftNode.getID()).get(i), 0);

        for(int i = 0; i < leftNode.getMustInclude().get(leftNode.getID()).size(); i++)
            leftNode.getRmp().resetCosntraint(leftNode.getMustInclude().get(leftNode.getID()).get(i), 1);

        //右节点包含该排班theta
        node.getMustInclude().get(node.getID()).add(branchingPoint);
        node.getRmp().initModel();
        node.getRmp().resetCosntraint(branchingPoint, 1);
        //节点应包含的所有约束
        for(int i = 0; i < node.getMustExclude().get(node.getID()).size(); i++)
            node.getRmp().resetCosntraint(leftNode.getMustExclude().get(node.getID()).get(i), 0);

        for(int i = 0; i < node.getMustInclude().get(node.getID()).size(); i++)
            node.getRmp().resetCosntraint(leftNode.getMustInclude().get(node.getID()).get(i), 1);

         */
        //左节点不包含该排班theta
        leftNode.getRmp().initModel();
        leftNode.getRmp().resetCosntraint(branchingPoint, 0);
        //右节点包含该排班theta
        node.getRmp().initModel();
        node.getRmp().resetCosntraint(branchingPoint, 1);

        nodes.add(node);
        nodes.add(leftNode);
        nodeCount += 2;
    }
}

