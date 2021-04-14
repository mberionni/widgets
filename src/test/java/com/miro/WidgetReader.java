package com.miro;

import com.miro.entities.Widget;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.Callable;

import static com.miro.TestUtils.msg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetReader {

    private final WidgetRepository repo;
    private final Random random = new Random();
    private static final int READ_ALL_INTERVAL = 2000;
    private static final int READ_SLEEP_INTERVAL = 2000;
    private static final int READ_SLEEP_MILLIS = 200;
    private volatile boolean running;
    private volatile List<Integer> zIndexes = Collections.emptyList();
    private volatile int numWidgets;
    private volatile boolean isWriterRunning;

    static class Results {
        int nGetAll;
        int nGetById;
        int nGetByIdNotFound;
        long nGetAllWidgets;
        long threadId;
        List<Widget> widgets;
        String errMsg = null;

        public Results(long threadId, int nGetAll, int nGetById, int nGetByIdNotFound, long totWidgets, String errMsg) {
            this.threadId = threadId;
            this.nGetAll = nGetAll;
            this.nGetById = nGetById;
            this.nGetByIdNotFound = nGetByIdNotFound;
            this.nGetAllWidgets = totWidgets;
            this.errMsg = errMsg;
        }

        public Results(long threadId, int nGetById, int nGetByIdNotFound, List<Widget> widgets) {
            this.threadId = threadId;
            this.nGetById = nGetById;
            this.nGetByIdNotFound = nGetByIdNotFound;
            this.widgets = widgets;
        }
    }

    static class SpecificResults {
        int hit;
        int missed;
        String errMsg;

        public SpecificResults(int hit, int missed, String errMsg) {
            this.hit = hit;
            this.missed = missed;
            this.errMsg = errMsg;
        }
    }

    public WidgetReader(WidgetRepository repo) {
        this.repo = repo;
    }

    public void init() {
        this.running = true;
    }

    private SortedSet<Widget> getWidgetsAll() {
        SortedSet<Widget> widgets;

        widgets = repo.findAll(null, null);
        return widgets;
    }

    private Results doRead() {
        int nGetAll = 0;
        int nGetById = 0;
        int nGetByIdNotFound = 0;
        long nGetAllWidgets = 0;
        String errMsg = null;

        long threadId = Thread.currentThread().getId();
        msg("doRead BEGIN, threadId: " + threadId);
        int i = 0;
        try {
            while (running) {
                if ((i % READ_ALL_INTERVAL) == 0) {
                    SortedSet<Widget> widgets = getWidgetsAll();
                    nGetAllWidgets += widgets.size();
                    nGetAll++;
                }
                long id = random.nextInt(numWidgets) + 1;
                Optional<Widget> opt = repo.findById(id);
                if(opt.isPresent()) {
                    nGetById++;
                    Widget w = opt.get();
                    assertEquals(id, w.getId(), "checking Id value");
                    if(isWriterRunning) {
                        // the writer will cause widgets to shift
                        assertTrue(w.getzIndex().longValue() >= id, "z-index was: " + w.getzIndex() + ", expected >= " + id);
                    } else {
                        assertEquals(id, w.getzIndex().longValue(), "checking z-index value");
                    }
                    assertEquals(id-1, w.getX().intValue(), "checking X value");
                    assertEquals(id-1, w.getY().intValue(), "checking Y value");
                } else {
                    nGetByIdNotFound++;
                }
                i++;
            }
        } catch (AssertionFailedError e) {
            errMsg = e.toString();
        }
        return new Results(threadId, nGetAll, nGetById, nGetByIdNotFound, nGetAllWidgets, errMsg);
    }

    private Results doReadAndCollect() {
        int i = 0;
        int size = 0;
        int nGetByIdNotFound = 0;
        int nGetById = 0;
        List<Widget> widgets = new ArrayList<>();

        long threadId = Thread.currentThread().getId();
        msg("doReadAndCollect BEGIN, threadId: " + threadId);
        while (running) {
            if (i == 0 || i % READ_SLEEP_INTERVAL == 0) {
                TestUtils.sleep(READ_SLEEP_MILLIS);
            }
            if (i % READ_ALL_INTERVAL == 0) {
                size = getWidgetsAll().size();
            }
            long id = size == 0 ? 1 : random.nextInt(size) + 1;
            Optional<Widget> opt = repo.findById(id);
            if (opt.isPresent()) {
                widgets.add(opt.get());
                nGetById++;
            } else {
                nGetByIdNotFound++;
            }
            i++;
        }
        return new Results(threadId, nGetById, nGetByIdNotFound, widgets);
    }

    private SpecificResults doReadSpecificWidgets() {
        long threadId = Thread.currentThread().getId();
        Optional<Widget> opt;
        Widget w;
        String errMsg = null;
        int hit = 0;
        int miss = 0;
        int size = repo.size();

        msg("doReadSpecificWidgets BEGIN, threadId: " + threadId);
        try {
            while (running) {
                for (int zIndex : zIndexes) {
                    /* using the zIndex as Id, we expect to retrieve the value that was created by the initial fetch */
                    opt = repo.findById(zIndex);
                    assertTrue(opt.isPresent());
                    w = opt.get();
                    assertEquals(zIndex-1, w.getX(), "checking X value");
                    assertEquals(zIndex-1, w.getY(), "checking Y value");
                    // IMPROVE: assumes zIndexes has 4 elements
                    assertTrue(zIndex == w.getzIndex() || zIndex + 1 == w.getzIndex() || zIndex + 2 == w.getzIndex() || zIndex + 3 == w.getzIndex() || zIndex + 4 == w.getzIndex(), "widget zindex check: " + w);
                }

                for (int i = size + 1; i <= size + zIndexes.size(); i++) {
                    int x = i - size;
                    /* using the zIndex as Id, we expect to retrieve the value that was created by the initial fetch */
                    opt = repo.findById(i);
                    if(opt.isPresent()) {
                        w = opt.get();
                        hit++;
                        assertEquals(x - 1, w.getX());
                        assertEquals(x - 1, w.getY());
                        assertEquals(zIndexes.get(x-1), w.getzIndex());
                    } else {
                        miss++;
                    }
                }
            }
        } catch (AssertionFailedError e) {
            errMsg = e.toString();
        }
        return new SpecificResults(hit, miss, errMsg);
    }

    public Callable<Results> read() {
        return this::doRead;
    }

    public Callable<Results> readAndCollect() {
        return this::doReadAndCollect;
    }

    public Callable<SpecificResults> readSpecificWidgets() {
         return this::doReadSpecificWidgets;
    }

    public WidgetReader setNumWidgets(int numWidgets) {
        this.numWidgets = numWidgets;
        return this;
    }

    public WidgetReader setZIndexes(List<Integer> zIndexes) {
        this.zIndexes = zIndexes;
        return this;
    }

    public WidgetReader setWriterRunning(boolean isWriterRunning) {
        this.isWriterRunning = isWriterRunning;
        return this;
    }

    public int getRepoSize() {
        return repo.size();
    }

    public SortedSet<Widget> getAllWidgets() {
        return repo.findAll(null, null);
    }

    public void stop() {
        msg("Stopping widget reader!");
        running = false;
    }
}