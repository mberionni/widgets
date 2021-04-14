package com.miro;

import com.miro.entities.Widget;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.miro.TestUtils.msg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WidgetsMultiThreadingTest {

    private static WidgetWriter widgetWriter;
    private static WidgetReader widgetReader;
    private static ExecutorService threadPool;
    private static WidgetRepository repo;
    private static final int TEST_EXEC_TIME_MILLIS = 10000;

    @BeforeAll
    static void setupAll() {
        repo = new WidgetMainRepository();
        threadPool = Executors.newCachedThreadPool();
        widgetWriter = new WidgetWriter(repo);
        widgetReader = new WidgetReader(repo);
    }

    @BeforeEach
    private void init() {
        widgetWriter.init();
        widgetReader.init();
    }

    @AfterEach
    private void cleanup() {
        repo.clear();
    }

    @AfterAll
    private static void tearDown() {
        msg("cleanup after-all");
        if (threadPool == null) {
            return;
        }
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void mainTestInternal(int iniSize, int nReaders, boolean isWriter) {
        List<Future<WidgetReader.Results>> readers = new ArrayList<>();
        Future<Integer> writer = null;
        Integer numWidgetsWriter = 0;
        int nGetAllTot = 0;
        int nGetByIdTot = 0;
        int nGetByIdNotFoundTot = 0;
        long nGetAllWidgetsTot = 0;

        msg("BEGIN - reading with " + nReaders + " thread(s), writer thread: " + isWriter);
        Widget.initSequence();
        widgetReader.setNumWidgets(iniSize)
                    .setWriterRunning(isWriter);

        // initial creation of widgets
        widgetWriter.initialFetch(iniSize);

        if(isWriter) {
            msg("starting the writer thread!");
            writer = threadPool.submit(widgetWriter.createWidgets());
        }
        for(int i=0; i < nReaders; i++) {
            msg("starting reading thread n: " + i);
            Future<WidgetReader.Results> reader = threadPool.submit(widgetReader.read());
            readers.add(reader);
        }
        msg("running....");
        TestUtils.sleep(TEST_EXEC_TIME_MILLIS);

        // stopping the reader and writer
        if (isWriter) {
            widgetWriter.stop();
        }
        widgetReader.stop();

        // get the results and clean up the repo
        for(Future<WidgetReader.Results> reader : readers) {
            WidgetReader.Results ret;
            try {
                ret = reader.get();
            } catch (InterruptedException | ExecutionException e) {
                msg("Error while waiting for reader future termination: " + e.getMessage());
                continue;
            }
            nGetAllTot += ret.nGetAll;
            nGetByIdTot += ret.nGetById;
            nGetByIdNotFoundTot += ret.nGetByIdNotFound;
            nGetAllWidgetsTot += ret.nGetAllWidgets;
            assertNull(ret.errMsg, "Test failed: " + ret.errMsg);
        }
        if(isWriter) {
            try {
                numWidgetsWriter = writer.get();
            } catch (InterruptedException | ExecutionException e) {
                msg("Error while waiting for writer future termination: " + e.getMessage());
            }
        }
        int size = widgetReader.getRepoSize();
        msg("--- Totals (repo size: " + size + ") ---");
        msg(nGetAllTot + ";" + nGetByIdTot + ";" + nGetByIdNotFoundTot + ";" + nGetAllWidgetsTot);

        // verify the results
        assertEquals(0, nGetByIdNotFoundTot);
        assertEquals(iniSize + numWidgetsWriter, size, "Repository size should be equal to the initial fetch size, plus the widgets created by the writer thread.");
        TestUtils.sleep(1500);
    }

    private void mainTestMultiWritersInternal(int nWriters) {
        Future<WidgetReader.Results> reader;
        List<Future<Integer>> writers = new ArrayList<>();
        WidgetReader.Results readerResults = new WidgetReader.Results(0, 0, 0, Collections.emptyList());
        int numWritersWidgets = 0;
        int nGetByIdNotFoundTot = 0;

        msg("BEGIN - num writer threads " + nWriters + ", 1 reader thread.");
        Widget.initSequence();

        // submit reader thread
        reader = threadPool.submit(widgetReader.readAndCollect());

        // submit the writer threads
        for(int i=0; i < nWriters; i++) {
            msg("starting the writing thread n: " + i);
            Future<Integer> writer = threadPool.submit(widgetWriter.createWidgetsWithSameZIndex());
            writers.add(writer);
        }
        msg("running...");
        TestUtils.sleep(TEST_EXEC_TIME_MILLIS);
        // stopping the reader and writer
        widgetWriter.stop();
        widgetReader.stop();
        TestUtils.sleep(800);

        try {
            readerResults = reader.get();
            nGetByIdNotFoundTot += readerResults.nGetByIdNotFound;
        } catch (InterruptedException | ExecutionException e) {
            msg("Error while waiting for future termination: " + e.getMessage());
        }
        for(Future<Integer> writer : writers) {
            try {
                numWritersWidgets += writer.get();
            } catch (InterruptedException | ExecutionException e) {
                msg("Error while waiting for reader future termination: " + e.getMessage());
            }
        }

        msg("verifying results...");
        assertEquals(0, nGetByIdNotFoundTot);

        int size1 = widgetReader.getRepoSize();
        msg("--- Final repo size: " + size1);
        // check that all widgets have unique z-index
        SortedSet<Widget> widgets = widgetReader.getAllWidgets();
        int size2 = widgets.size();
        assertEquals(size1, size2, "Check consistency of the backing data structures");
        assertEquals(numWritersWidgets, size1, "Check consistency of the backing data structures");
        verifyAllWidgets(widgets, readerResults.widgets, size1);
    }

    private void verifyAllWidgets(SortedSet<Widget> allWidgets, List<Widget> readerWidgets, int size) {
        for (Widget w : readerWidgets) {
            assertTrue(allWidgets.contains(w));
        }
        int i = 1;
        for(Widget w : allWidgets) {
            assertEquals(i, w.getX());
            assertEquals(i, w.getY());
            assertEquals(i, w.getzIndex());
            i++;
            if(i == 10) {
                break;
            }
        }
        Widget last = allWidgets.last();
        assertEquals(size, last.getzIndex());
    }

    private void mainTestReadingSpecificWidget(int iniSize, int nReaders) {
        List<Future<WidgetReader.SpecificResults>> readers = new ArrayList<>();
        Future<Integer> writer;
        int totHits = 0;
        int totMiss = 0;
        int numWidgetsCreated = 0;
        List<Integer> zIndexes = List.of(100, 200, 300, 400);

        msg("BEGIN - num reader (specific value) threads " + nReaders + ", 1 writer thread.");
        Widget.initSequence();
        widgetReader.setNumWidgets(iniSize)
                    .setZIndexes(zIndexes);
        widgetWriter.setZIndexes(zIndexes);

        // initial creation of widgets
        widgetWriter.initialFetch(iniSize);

        // submit writer thread
        writer = threadPool.submit(widgetWriter.createSpecificWidgets());
        // submit reader threads
        for(int i=0; i < nReaders; i++) {
            msg("starting reading thread n: " + i);
            Future<WidgetReader.SpecificResults> reader = threadPool.submit(widgetReader.readSpecificWidgets());
            readers.add(reader);
        }

        msg("running...");
        TestUtils.sleep(TEST_EXEC_TIME_MILLIS);
        // stopping the reader and writer
        widgetWriter.stop();
        widgetReader.stop();
        TestUtils.sleep(800);

        msg("verifying results...");

        for(Future<WidgetReader.SpecificResults> reader : readers) {
            WidgetReader.SpecificResults ret;
            try {
                ret = reader.get();
            } catch (InterruptedException | ExecutionException e) {
                msg("Error while waiting for reader future termination: " + e.getMessage());
                continue;
            }
            totHits += ret.hit;
            totMiss += ret.missed;
            assertNull(ret.errMsg, "Test failed: " + ret.errMsg);
        }
        try {
            numWidgetsCreated = writer.get();
        } catch (InterruptedException | ExecutionException e) {
            msg("Error while waiting for reader future termination: " + e.getMessage());
        }

        int size1 = widgetReader.getRepoSize();
        msg("--- Readers threads. Hits: " + totHits + " Miss: " + totMiss);
        msg("--- Initial repo size: " + iniSize + " Final repo size: " + size1);
        SortedSet<Widget> widgets = widgetReader.getAllWidgets();
        int size2 = widgets.size();
        assertEquals(size1, size2, "Check consistency of the backing data structures");
        assertEquals((iniSize + numWidgetsCreated), size2, "Check total widgets");

        assertTrue(totHits > 0);
        assertTrue(totMiss > 0);
        assertTrue(totHits > totMiss);
    }

    @Test
    @Order(1)
    /* we want the warmup to be the first test to run */
    public void testWarmup() {
        int iniSize = 1000000;
        int nReaders = 1;

        msg("BEGIN - warmup - reading single thread, no writer");
        mainTestInternal(iniSize, nReaders, false);
        msg("END - warmup -");
    }

    @Test
    @Order(2)
    public void testReadingSingleThreadWhileWriting() {
        int iniSize = 2000000;
        int nReaders = 1;
        mainTestInternal(iniSize, nReaders, true);
    }

    @Test
    @Order(3)
    public void testReadingSingleThread() {
        int iniSize = 2000000;
        int nReaders = 1;
        mainTestInternal(iniSize, nReaders, false);
    }

    @Test
    @Order(4)
    public void testReadingMultiThreadWhileWriting() {
        int iniSize = 2000000;
        int nReaders = 4;
        mainTestInternal(iniSize, nReaders, true);
    }

    @Test
    @Order(5)
    public void testReadingMultiThread() {
        int iniSize = 2000000;
        int nReaders = 4;
        mainTestInternal(iniSize, nReaders, false);
    }

    @Test
    @Order(6)
    public void testReadingThreadWhileMultiWritingWithShift() {
        int nWriters = 4;
        mainTestMultiWritersInternal(nWriters);
    }

    @Test
    @Order(7)
    public void testReadingSpecificWidgetWhileWritingIt() {
        int nReaders = 4;
        int iniSize = 10000;
        mainTestReadingSpecificWidget(iniSize, nReaders);
    }
}