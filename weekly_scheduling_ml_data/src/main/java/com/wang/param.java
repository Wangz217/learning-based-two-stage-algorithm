package com.wang;

/**
 * @author: wzx
 * Date: 2021-10-01
 * Description: com.wang
 * Version: 1.0
 */
public class param {
    public static int days = 7;
    public static int hours = 24;
    public static int T = 168;	//days * 24
    public static int delta = 1;
    public static int Num = 5;  //医生数量
    public static int N = 8;		//排班数量
    public static int lengthOfShift = 8;		//排班的时长

    public static double mu = 11.58;
    public static double mu_aux = 2.25;
    public static int c_aux = 25;
    public static double prob = 0.62;   //去检查的概率

    public static int C_max = 3;
    public static int C_min = 0;
    public static int H = 50;
    public static final double err = 1e-4;
    public static int V  = 60;
    public static double Limit  = 25;
    public static double LsLimit  = 8;
    public static double LsMax  = 100;

    public static int expNum = 1000;

//    public static double[][] lam = {
//            //第1/2/3周
//            {2,2.5,2,5.5,6,8,7.5,7,6,5.5,3.6,3.4,2.5,2.65,1.5,1,1.2,1.35,2.35,1.6,3.5,5.1,7.05,4.95,2,2,1.65,4.35,6.6,8.55,7.5,7.75,6.55,5,3.6,3.4,2.5,2.65,1.5,1,0.7,1.35,2.35,1.6,3.5,4.95,5.1,7.05,3.5,2,1.65,4.35,6.6,7.5,8.55,7.75,6.55,5,2.55,3,2,2.2,1.55,0.75,1.05,1.5,3.3,2.5,2.1,2.75,5,6,4.45,3.5,3.5,4.45,6.5,6.45,7.5,5.75,5.35,5.6,3.05,2.75,3.6,1.65,0.95,1.5,1.05,2.85,2.05,2,3.95,4.5,5.75,6,3,2.05,1.65,4.6,5.95,6.7,7.5,6.5,6.15,4.5,2.5,2.65,1.8,2,1,2,2.15,3,2,4.5,4.5,6,5.5,6.55,3.5,2.5,2.2,4.5,6.05,7.05,6.55,6.05,6.15,4,2,1.5,1.8,1.55,1,1,1.55,3.5,6,5.5,4.5,4,5,3,3.5,2.5,2.5,5.5,6.05,7.05,5.5,5,4.5,3.5,3.5,3,2,2.5,1,1,0.5,1.5,2.5,2,2.5,5.5,6.5,4},
//            {2.1,1.7,2.25,4,6,7.8,6.55,5.95,5.15,4.25,3.65,1.9,2.5,5.2,0.8,0.75,0.85,1.2,3.6,2.25,2.95,5.85,9.4,6.6,4,2.35,1.4,4.85,8,9.1,5.9,6.8,6.3,6.6,2.75,2.9,2.55,1.2,1.1,0.55,0.65,1.25,3.4,1.6,4.15,4.65,6.4,8.55,4.5,2.2,1.65,3.6,7.6,6.3,7.9,7.75,5.35,3.5,1.85,2.25,3.1,2.85,0.9,0.85,1.1,1.55,3.3,3.6,2,3.1,4.45,5.9,4,2.8,3.55,3.85,7.05,5.95,8.1,6.05,5.05,3.55,2.15,1.7,1.55,0.75,0.55,0.85,0.75,3.05,2.3,2.45,4.1,4.65,5.1,4.25,2.4,1.8,3.15,4.2,3.6,3.65,6.3,5.45,4.4,4.4,3.8,3.65,3,2.4,1,1.25,1.95,2.6,2.65,3.55,3.65,3.35,5.4,5.05,4.05,2,1.65,5.05,5.8,5,5.95,5.4,3.65,3.8,1.55,2.2,1.45,1.2,1,0.6,1,4.1,4.1,4.55,4.15,4.05,3.35,2.15,2.7,1.95,2.15,3.4,4.45,3.95,4.65,5,3.55,2.2,2.9,1.8,1.65,3.15,0.9,0.75,0.35,1.55,1.9,2.1,2.35,2.3,4.3,3.55},
//            {2.55,2.4,2.2,5.9,5.35,9.75,8.1,6.5,5.35,7.25,3.65,2.8,2.75,4,1.75,0.9,1.1,1.3,2.4,2,1.7,4.65,7.05,4,3.6,1.75,1.15,3.85,6,8,8.4,6.65,5.25,5.25,2.75,2.3,2.85,1.7,1.25,0.7,1.2,1.2,4.1,2.1,3.5,5.1,5.4,7.85,3.75,1.6,1.3,2.8,5.35,6.6,7,6.9,6.6,4.25,2.4,2.25,2.8,3.45,1,0.9,1.2,1.75,2.85,2.3,2,2.85,5,6.15,3.15,4.5,3,2.9,6.7,5.65,6.85,4.95,7.65,3.9,2.4,3.1,2.3,1.4,0.65,1,1.15,2.9,1.6,1.8,3.1,5,5.55,3.55,2.2,2,1.15,4,4,5.15,5.15,5,6.2,3.55,2.4,1.6,1.9,2.35,0.4,1.65,2.85,2.65,2.1,1.95,4.8,3.05,4.25,4.1,2.6,1.5,1.85,3.8,3.55,6.9,4.35,5.2,6.05,3.05,2.1,0.5,2.8,2.8,1.2,0.7,1.35,4.65,4.25,4.1,3.85,3.1,3.05,3.3,2.15,2.2,2.2,3.8,2.95,4.8,4.45,4.4,4.5,2.15,2.9,2.2,2.3,2,2.1,0.8,0.45,1.4,2.1,2.75,2.8,3.45,4.5,3.1}
//
//    };

    public static double[][] warmStart = {
            //第1/2/3周
            {1,1,1,1,1.99,1.99,2,2,2,2,2,2,1.01,1.01,1,1,1,1,1,1,1,1,1,1,1,1.86,2,2.01,2.73,2.73,3,3,3,2.14,2,1.99,1.27,1.27,1,1,1,1,1,1,1,1,1,1,1,1.17,1.27,2,2.14,2.14,3,3,3,2.83,2.73,2,1.86,1.86,1,1,1,1,1,1,1,1,1,1,1,1.92,2,2,2.02,2.02,3,3,3,2.08,2,2,1.98,1.98,1,1,1,1,1,1,1,1,1,1,1,1.79,1.87,2,2.06,2.06,3,3,3,2.21,2.13,2,1.94,1.94,1,1,1,1,1,1,1,1,1,1,1,1.77,1.89,1.97,2.04,2.04,3,3,3,2.23,2.11,2.03,1.96,1.96,1,1,1,1,1,1,1,1,1,1,1,1.03,1.04,1.04,1.92,1.92,2.04,2.04,2.04,2.01,2,2,1.12,1.12,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,1,0,1,1,1,0.4,0.6,0,0.2,0.8,1,1,1,0.4,0.2,0.4,0.2,0.8,1,1,1,0.8,0.2,0,0,1,1,1,1,0.6,0,0.4,0.2,0.8,1,1,1,0.8,0,0.2,0,1,1,1,1,0,0.2,0,0.6,0.4,1,1}
    };

}
