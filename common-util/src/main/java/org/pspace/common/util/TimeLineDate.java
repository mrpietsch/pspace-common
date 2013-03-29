package org.pspace.common.util;


import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author peach
 */
public class TimeLineDate<T> {

    public TimeLineDate(Mode mode, ScaleStrategy<T> strategy) {
        this.mode = mode;
        this.strategy = strategy;
        this.entries = new TreeMap<Date, T>();
    }

    public TimeLineDate(Mode mode, ScaleStrategy<T> strategy, Map<Date, T> entries) {
        this.mode = mode;
        this.strategy = strategy;
        this.entries = new TreeMap<Date, T>(entries);
    }

    public enum Mode implements Comparable<Mode> {
        QUARTER_HOUR(Calendar.MINUTE, 15),
        HOUR(Calendar.HOUR_OF_DAY, 1),
        DAY(Calendar.DATE, 1);

        private final int unit;
        private final int count;

        Mode(int unit, int count) {
            this.unit = unit;
            this.count = count;
        }

        public Date mapToBucket(Date date) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            switch (this) {
                case DAY:
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                case HOUR:
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    break;

                case QUARTER_HOUR:
                    // round to the next quarter
                    calendar.set(Calendar.MINUTE, (calendar.get(Calendar.MINUTE) / 15) * 15);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    break;
            }
            return calendar.getTime();
        }

    }


    public void setValueOnlyAt(Date date, T value) {
        Map.Entry<Date, T> lower = this.entries.lowerEntry(date);
        if (lower == null || !lower.getValue().equals(value)) {
            this.entries.put(date, value);
        }
    }

    @SuppressWarnings("unchecked")
    public TimeLineDate<T> scaledCopy(Mode targetMode) {
        if (this.mode == targetMode) {
            try {
                return (TimeLineDate<T>) this.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);

            }
        } else {

                TimeLineDate<T> tl = new TimeLineDate<T>(targetMode, strategy);

            if (this.mode.compareTo(targetMode) > 0) {
                // exploding is easy, we don't have to look ahead for other entries to
                // join and just can explode
                for (Map.Entry<Date, T> entry : entries.entrySet()) {
                    tl.setValueAt(entry.getKey(), strategy.explode(this.mode, targetMode, entry.getValue()));
                }
            } else {

                Date currentBucket = null;
                Map<Date, T> map = null;

                ExecutorService executorService = Executors.newFixedThreadPool(2);

                for (Map.Entry<Date, T> entry : entries.entrySet()) {
                    Date myBucket = targetMode.mapToBucket(entry.getKey());
                    if (!myBucket.equals(currentBucket)) {
                        if (currentBucket != null) {
                            T agg = strategy.aggregate(mode, targetMode, map);
                            tl.setValueAt(currentBucket, agg);
                        }
                        currentBucket = myBucket;
                        map = new HashMap<Date, T>(4);
                    }
                    map.put(entry.getKey(), entry.getValue());

                }

                if (currentBucket != null) {
                    T agg = strategy.aggregate(mode, targetMode, map);
                    tl.setValueAt(currentBucket, agg);
                }
            }

            return tl;

        }
    }


    public static interface ScaleStrategy<T> {
        T aggregate(Mode from, Mode to, Map<Date, T> source);
        T explode(Mode from, Mode to, T source);
    }

    final Mode mode;
    final ScaleStrategy<T> strategy;

    protected NavigableMap<Date, T> entries;


    public T getValueAt(Date date) {
        Map.Entry<Date, T> x = this.entries.lowerEntry(date);
        return x.getValue();
    }

    public void setValueAt(Date date, T value) {
        Date key = mode.mapToBucket(date);
        Map.Entry<Date, T> lower = this.entries.lowerEntry(key);
        if (lower == null || !lower.getValue().equals(value)) {
            this.entries.put(key, value);
        }
    }

    public void setValueAndClearFuture(Date date, T value) {
        this.setValueAt(mode.mapToBucket(date), value);
        this.entries = this.entries.headMap(date, true);
    }

    public SortedMap<Date, T> getEntries() {
        return Collections.unmodifiableSortedMap(entries);
    }

    public Date getStartDate() {
        return entries.firstEntry().getKey();
    }

    public Date getLatestDate() {
        return entries.lastEntry().getKey();
    }

    public int size() {
        return this.entries.size();
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TimeLineDate");
        sb.append("{entries=").append(entries);
        sb.append('}');
        return sb.toString();
    }
}