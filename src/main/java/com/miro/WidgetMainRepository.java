package com.miro;

import com.miro.entities.Widget;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WidgetMainRepository implements WidgetRepository {

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();
    /* HashMap is used to find by id (constant time on average) */
    Map<Long, Widget> widgetsMap = new ConcurrentHashMap<>();
    /*
    ** SortedSet is used to keep the widgets ordered by z-index.
    ** It supports the findAll and findMaxZIndex.
    ** It is also used to shift widgets efficiently (when necessary)
    */
    SortedSet<Widget> widgetsZIndex = new ConcurrentSkipListSet<>();

    @Override
    public void save(Widget widget) {
        /* checks if there is already a widget with the same z-index */
        boolean exists = widgetsZIndex.contains(widget);
        if (exists) {
            saveAndShift(widget);
            return;
        }
        widgetsMap.put(widget.getId(), widget);
        widgetsZIndex.add(widget);
    }

    private void saveAndShift(Widget widget) {
        // synchronization to guarantee atomic update
        writeLock.lock();
        try {
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
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void update(Widget widget, Widget newWidget) {
        // synchronization to guarantee atomic update
        writeLock.lock();
        try {
            widgetsZIndex.remove(widget);
            save(newWidget);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Widget> findById(long id) {
        readLock.lock();
        try {
            Widget widget = widgetsMap.get(id);
            if (widget == null) {
                return Optional.empty();
            }
            return Optional.of(widget);
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Widget> findAll(Integer size, Integer page) {
        List<Widget> ret = new ArrayList<>();

        readLock.lock();
        try {
            if (page == null || size == null) {
                // alternatively, if necessary, a deep copy can be returned: sortedRepo.stream().map(Widget::new).collect(Collectors.toList());
                ret.addAll(widgetsZIndex);
                return ret;
            }
            /* support for pagination */
            int begin = (page * size) - size;
            int end = page * size;

            if (begin > widgetsZIndex.size()) {
                /* returns an empty list if the page is greater than total number of pages */
                return ret;
            }
            int count = 0;
            for (Widget widget : widgetsZIndex) {
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
        finally {
            readLock.unlock();
        }
    }

    @Override
    public void deleteById(long id) {
        Widget widget = widgetsMap.get(id);
        if(widget == null) {
            /* do nothing if the widget does not exists */
            return;
        }
        widgetsMap.remove(id);
        widgetsZIndex.remove(widget);
    }

    @Override
    public Widget findMaxZIndex() {
        if(widgetsZIndex.isEmpty()) {
            return null;
        }
        return widgetsZIndex.last();
    }

    @Override
    public int size() {
        return widgetsMap.size();
    }

    @Override
    public void clear() {
        widgetsMap.clear();
        widgetsZIndex.clear();
    }
}