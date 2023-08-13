package com.wang.twoStage_sp.util;

import java.util.*;

/**
 * @author: wzx
 * Date: 2021-10-29
 * Description: com.wang.twoStage_sp.util
 * Version: 1.0
 */
public class Copier {

    public static<E,F> Map<E, List<F>> copyMapOfList(Map<E, List<F>> map) {
        Map<E, List<F>> copy = new HashMap<>();
        for(Map.Entry<E, List<F>> entry: map.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<F>(entry.getValue()));
        }
        return copy;
    }

    public static<E,F> Map<E, Set<F>> copyMapOfSet(Map<E, Set<F>> map) {
        Map<E, Set<F>> copy = new HashMap<>();
        for(Map.Entry<E, Set<F>> entry: map.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}