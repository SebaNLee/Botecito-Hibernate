package ar.edu.itba.paw.services.utils;

import java.util.ArrayList;
import java.util.Collection;

public class TimeRangeList extends ArrayList<TimeRange> {

    private boolean canAdd(TimeRange newRange) {
        for (TimeRange range : this) {
            if (range.intersects(newRange)) return false;
        }
        return true;
    }

    private void throwException() {
        throw new IllegalArgumentException("Cannot insert a TimeRange that intersects with an existing one");
    }

    @Override
    public boolean add(TimeRange range) {
        if (!canAdd(range)) throwException();
        return super.add(range);
    }

    @Override
    public void add(int index, TimeRange range) {
        if (!canAdd(range)) throwException();
        super.add(index, range);
    }

    @Override
    public boolean addAll(Collection<? extends TimeRange> c) {
        for (TimeRange timeRange : c) {
            if (!canAdd(timeRange)) throwException();
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends TimeRange> c) {
        for (TimeRange timeRange : c) {
            if (!canAdd(timeRange)) throwException();
        }
        return super.addAll(index, c);
    }
}
