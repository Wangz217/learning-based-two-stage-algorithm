package com.wang.twoStage_sp.util;

/**
 * @author: wzx
 * Date: 2021-10-29
 * Description: com.wang.twoStage_sp.util
 * Version: 1.0
 */
public class Coder {
    public static String encodeCOM(int c, int o, int m) {
        StringBuilder sb = new StringBuilder();
        sb.append(c);
        sb.append("#");
        sb.append(o);
        sb.append("#");
        sb.append(m);
        return sb.toString();
    }
}
