package com.wang;
import static com.wang.param.*;
/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang.twoStage_mp.util
 * Version: 1.0
 */
public class BPSFFA {
    // factorial
    private static double fact(int i) {
        if (i <= 1) return 1;
        return i * fact(i - 1);
    }

    private static double Len_MMC(double rho, int c) {
        double sigma = 0;
        for (int i = 0; i < c; i++) {
            sigma += Math.pow(rho * c, i) / fact(i);
        }
        double P0 = 1 / (sigma + (Math.pow(rho * c, c) / (fact(c) * (1 - rho))));
        double Ls = Math.pow(rho, c + 1) * Math.pow(c, c) * P0 / (fact(c) * Math.pow(1 - rho, 2)) + rho * c;
        return Ls;
    }

    public static double getQueueLength(double L0, double lamda, double mu, int c, double dt) {
        double bmin = 0;
        double bmax = 1;
        double Ls;
        while (true) {
            double rho = bmin + (bmax - bmin) / 2.0;
            Ls = Len_MMC(rho, c);
            double left = Ls;
            double right = L0 + lamda * dt - mu * c * rho * dt;
            if (Math.abs(left - right) <= 0.001)
                break;
            else if (left > right)
                bmax = rho;
            else
                bmin = rho;
        }
        return Ls;
    }

    public static double findRange(int c, double target){
        double r = 0;
        double L = 0;
        double r_max = 1;
        double r_min = 0;
        boolean flag = true;
        while(flag){
            r = 0.5 * (r_max+r_min);
            L = Len_MMC(r,c);
            if (Math.abs(L - target) <= err) flag = false;
            else{
                if (L > target)
                    r_max = r;
                else
                    r_min = r;
            }
        }
        return r;
    }
}
