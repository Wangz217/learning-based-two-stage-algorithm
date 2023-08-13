package com.wang;

/**
 * @author: wzx
 * Date: 2021-10-02
 * Description: com.wang
 * Version: 1.0
 */
public class Timer {
    private static long start;

    public static void reset() {
        start = System.currentTimeMillis();
    }

    public static boolean reachEnd(double delay) {
        if ((System.currentTimeMillis() - start) / 1000 > delay) return true;
        return false;
    }
}
