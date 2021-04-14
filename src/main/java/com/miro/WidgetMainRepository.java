package com.miro;

import com.miro.entities.Point;
import com.miro.entities.Widget;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

public class WidgetMainRepository implements WidgetRepository {

    private final StampedLock sl = new StampedLock();
    /* HashMap is used to find by id (constant time on average) */
    private final Map<Long, Widget> widgetsMap = new HashMap<>();
    /*
    ** SortedSet is used to keep the widgets ordered by z-index.
    ** It supports the findAll and findMaxZIndex.
    ** It is also used to shift widgets efficiently (when necessary)
    */
    private final SortedSet<Widget> widgetsZIndex = new TreeSet<>();
    /*
    ** Comparator used to have a data structure ordered by the X coordinate of the widget equal if it has the same id.
    ** 2 widgets are equal if the have the same Id (or same z-index).
    */
    private final Comparator<Widget> compX = (w1, w2) -> {
        if(w1.getId().intValue() == w2.getId().intValue()) {
            return 0;
        }
        return (w1.getX() < w2.getX()) ? - 1 : 1;
    };
    private final SortedSet<Widget> widgetsX = new TreeSet<>(compX);
    private static final AtomicLong sequence = new AtomicLong();

    @Override
    public void save(Widget widget) {
        saveInternal(widget, false);
    }

    private void saveInternal(Widget widget, boolean writeLockAcquired) {
        long stamp = 0;
        boolean exists;

        if(!writeLockAcquired) {
            // synchronization to guarantee atomic update
            stamp = sl.writeLock();
        }
        try {
            if (widget.getzIndex() == null) {
                widget.setzIndex(getNextZIndex());
                exists = false;
            } else {
                exists = widgetsZIndex.contains(widget);
            }
            // the Id is already set in case of update
            if (widget.getId() == null) {
                widget.setId(sequence.incrementAndGet());
            }
            widget.setModificationDate(LocalDateTime.now());
            if (exists) {
                saveAndShift(widget);
            } else {
                widgetsZIndex.add(widget); // adds to the set if not already present
                widgetsMap.put(widget.getId(), widget); // if the key is already present, the value is replaced
                widgetsX.add(widget); // adds to the set if not already present
            }
        }
        finally {
            if (!writeLockAcquired) {
                sl.unlockWrite(stamp);
            }
        }
    }

    private void saveAndShift(Widget widget) {
        // update z-index of following widgets (if any)
        SortedSet<Widget> w_subset = widgetsZIndex.tailSet(widget);
        int previous_z = w_subset.first().getzIndex();
        for(Widget w : w_subset) {
            int current_z = w.getzIndex();
            if(current_z > previous_z + 1) {
                break;
            }
            previous_z = current_z;
            w.setzIndex(w.getzIndex() + 1);
            w.setModificationDate(LocalDateTime.now());
            widgetsZIndex.add(w);
        }
        widgetsMap.put(widget.getId(), widget);
        widgetsZIndex.add(widget);
        widgetsX.add(widget);
    }

    @Override
    public void update(Widget widget, Widget newWidget) {
        // synchronization to guarantee atomic update
        long stamp = sl.writeLock();
        try {
            widgetsZIndex.remove(widget);
            widgetsX.remove(widget);
            saveInternal(newWidget, true);
        }
        finally {
            sl.unlockWrite(stamp);
        }
    }

    @Override
    public Optional<Widget> findById(long id) {
        Widget widget;
        long stamp;
        // acquire read lock only if the optimistic read "failed"
        stamp = sl.tryOptimisticRead();
        widget = widgetsMap.get(id);
        if (!sl.validate(stamp)) {
            stamp = sl.readLock();
            try {
                widget = widgetsMap.get(id);
            } finally {
                sl.unlockRead(stamp);
            }
        }
        if (widget == null) {
            return Optional.empty();
        }
        return Optional.of(widget);
    }

    @Override
    public SortedSet<Widget> findAll(Integer page_size, Integer page_num) {
        return findAll(page_size, page_num, null, null);
    }

    @Override
    public SortedSet<Widget> findAllInRectangle(Point lowerLeft, Point upperRight) {
        // This return set is ordered by zIndex (natural order of Widget)
        SortedSet<Widget> ret = new TreeSet<>();

        if (lowerLeft == null || upperRight == null) {
            return widgetsZIndex;
        }
        /* TODO can be optimized using the optimistic lock, as with getById */
        long stamp = sl.readLock();
        try {
            int count = 0;
            for (Widget widget : widgetsX) {
                if (widget.getX() >= upperRight.getX()) {
                    System.out.println("search for area terminated after " + count + " iterations.");
                    break;
                }
                if (includeWidget(widget, lowerLeft, upperRight)) {
                    ret.add(widget);
                }
                count++;
            }
        } finally {
            sl.unlockRead(stamp);
        }
        return ret;
    }

    @Override
    public SortedSet<Widget> findAll(Integer page_size, Integer page_num, Point lowerLeft, Point upperRight) {
        SortedSet<Widget> ret;
        long stamp;

        // acquire read lock only if the optimistic read "failed"
        stamp = sl.tryOptimisticRead();
        ret = findAllInternal(page_size, page_num, lowerLeft, upperRight);
        if (!sl.validate(stamp)) {
            stamp = sl.readLock();
            try {
                ret = findAllInternal(page_size, page_num, lowerLeft, upperRight);
            }
            finally {
                sl.unlockRead(stamp);
            }
        }
        return ret;
    }

    private SortedSet<Widget> findAllInternal(Integer page_size, Integer page_num, Point lowerLeft, Point upperRight) {
        SortedSet<Widget> ret = new TreeSet<>();
        int begin;
        int end;

        boolean pageFilter = page_num != null && page_size != null;
        boolean areaFilter = lowerLeft != null && upperRight != null;
        if (!pageFilter && !areaFilter) {
            // alternatively, if necessary, a deep copy can be returned:
            // ret = widgetsZIndex.stream().map(Widget::new).collect(Collectors.toList());
            return widgetsZIndex;
        }

        begin = 0;
        end = widgetsZIndex.size();
        /* support for pagination */
        if(pageFilter) {
            begin = (page_num * page_size) - page_size;
            end = page_num * page_size;
        }

        if (begin > widgetsZIndex.size()) {
            /* returns an empty set if the requested page is greater than total number of pages */
            return Collections.emptySortedSet();
        }
        int count = 0;
        for (Widget widget : widgetsZIndex) {
            if(areaFilter && !includeWidget(widget, lowerLeft, upperRight)) {
                continue;
            }
            if (count < begin) {
                count++;
                continue;
            }
            if (count >= end) {
                break;
            }
            ret.add(widget);
            count++;
        }
        return ret;
    }

    private boolean includeWidget(Widget widget, Point lowerLeft, Point upperRight) {
        return (widget.getX() >= lowerLeft.getX()) &&
               (widget.getX() + widget.getWidth() <= upperRight.getX()) &&
               (widget.getY() >= lowerLeft.getY()) &&
               (widget.getY() + widget.getHeight() <= upperRight.getY());
    }

    @Override
    public void deleteById(long id) {
        long stamp = sl.writeLock();
        try {
            Widget widget = widgetsMap.get(id);
            if (widget == null) {
                /* do nothing if the widget does not exists */
                return;
            }
            widgetsMap.remove(id);
            widgetsZIndex.remove(widget);
            widgetsX.remove(widget);
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    /* This function has to be used in an already thread-safe context */
    private int getNextZIndex() {
        int ret;
        try {
            ret = widgetsZIndex.last().getzIndex() + 1;
        } catch (NoSuchElementException e) {
            ret = 1;
        }
        return ret;
    }

    @Override
    public void initSequence() {
        sequence.set(0);
    }

    @Override
    public int size() {
        return widgetsMap.size();
    }

    @Override
    public void clear() {
        widgetsMap.clear();
        widgetsZIndex.clear();
        widgetsX.clear();
    }
}