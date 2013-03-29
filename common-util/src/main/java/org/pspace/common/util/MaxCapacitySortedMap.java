package org.pspace.common.util;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 *@author peach
 */
public class MaxCapacitySortedMap<K,V> extends TreeMap<K,V> {

    private static final long serialVersionUID = -8977820259655930087L;

    private final int maxCapacity;

    public MaxCapacitySortedMap(int max) {
        super();
        this.maxCapacity = max;
    }
    public MaxCapacitySortedMap(int max, Comparator<K> comparator) {
        super(comparator);
        this.maxCapacity = max;
    }
    public MaxCapacitySortedMap() {
        super();
        this.maxCapacity = Integer.MAX_VALUE;
    }
    public MaxCapacitySortedMap(Comparator<K> comparator) {
        super(comparator);
        this.maxCapacity = Integer.MAX_VALUE;
    }

    @Override
    public V put(K k, V v) {
        V ret = super.put(k, v);
        if (size()>maxCapacity) this.pollLastEntry();
        return ret;

    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        super.putAll(map);
        while (size()>maxCapacity) this.pollLastEntry();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

}
