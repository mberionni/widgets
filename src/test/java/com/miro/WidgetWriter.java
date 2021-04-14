package com.miro;

import com.miro.entities.Widget;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WidgetWriter {
    private static final long sleepIntervalMillis = 200;
    private static final int SLEEP_BATCH = 200;
    private static final int Z_INDEX_BATCH = 10;
    private volatile boolean running;
    private final WidgetRepository repo;
    private int size = 0;
    private List<Integer> zIndexes = Collections.emptyList();

    public WidgetWriter(WidgetRepository repo) {
        this.repo = repo;
    }

    public void init() {
        running = true;
    }

    public void initialFetch(int initialSize) {
        TestUtils.msg("Initial fetch, " + initialSize + " widgets!");
        for (int i=0; i < initialSize; i++) {
            Widget w = Widget.of(i, i, 10, 10, null, repo);
            repo.save(w);
        }
        size = initialSize;
    }

    /* creates new widgets, executed in a thread */
    private int doCreateWidgets() {
        TestUtils.msg("Thread Id: " + Thread.currentThread().getId() + ": starting create widgets task.");
        int i = size;
        while(running) {
            if ((i % SLEEP_BATCH) == 0) {
                TestUtils.sleep(sleepIntervalMillis);
            }
            Widget w = Widget.of(i, i, 10, 10, null, repo);
            repo.save(w);
            i++;
            if ((i % Z_INDEX_BATCH) == 0) {
                /* z-index already existing, will acquire a write lock */
                w = Widget.of(i, i, 10, 10, i - 12000, repo);
                repo.save(w);
                i++;
            }
        }
        TestUtils.msg("Thread Id: " + Thread.currentThread().getId() + ": completed creation task. Num widgets created: " + (i - size));
        return  i - size;
    }

    private int doCreateSpecificWidgets() {
        TestUtils.msg("Thread Id: " + Thread.currentThread().getId() + ": starting create specific widgets task.");
        int i = 0;
        while(running && i < zIndexes.size()) {
            TestUtils.sleep(TimeUnit.SECONDS.toMillis(1));
            int zIndex = zIndexes.get(i);
            Widget w = Widget.of(i, i, 10, 10, zIndex, repo);
            repo.save(w);
            i++;
        }
        TestUtils.msg("Thread Id: " + Thread.currentThread().getId() + ": completed creation task. Num widgets created: " + i);
        return i;
    }

    private int doCreateWidgetsWithSameZIndex() {
        TestUtils.msg("Thread Id: " + Thread.currentThread().getId() + ": starting create widgets task.");
        int i = 1;
        while(running) {
            Widget w = Widget.of(i, i, 10, 10, Math.min(i, 10), repo);
            repo.save(w);
            if ((i % SLEEP_BATCH) == 0) {
                TestUtils.sleep(sleepIntervalMillis);
            }
            i++;
        }
        TestUtils.msg("Thread Id: " + Thread.currentThread().getId() + ": completed creation task. Num widgets created: " + (i - 1));
        return i - 1;
    }

    void stop() {
        TestUtils.msg("Stopping the widget writer!");
        running = false;
    }

    void setZIndexes(List<Integer> zIndexes) {
        this.zIndexes = zIndexes;
    }

    Callable<Integer> createWidgets() {
        return this::doCreateWidgets;
    }

    Callable<Integer> createSpecificWidgets() {
        return this::doCreateSpecificWidgets;
    }

    Callable<Integer> createWidgetsWithSameZIndex() {
        return this::doCreateWidgetsWithSameZIndex;
    }
}
