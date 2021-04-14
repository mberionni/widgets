package com.miro;

import com.miro.entities.Point;
import com.miro.entities.Widget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.ThreadLocalRandom;

import static com.miro.TestUtils.msg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetsPerformanceTest {

    private static final WidgetRepository repo = new WidgetMainRepository();

    @BeforeEach
    private void setup() {
        repo.initSequence();
    }

    @AfterEach
    private void cleanup() {
        repo.clear();
    }

    private void fetchData(int size) {
        for (int i = 0; i < size; i++) {
            Widget w = Widget.of(i, i, 10, 10, null);
            repo.save(w);
        }
        msg("created " + size + " widgets.");
    }

    private void testGetByIdInternal(int size) {
        msg("-------- get by Id ------------");
        msg("fetched " + size + " widgets. Total number of widgets: " + repo.size());

        int id = ThreadLocalRandom.current().nextInt(0, size + 1);
        long ini = System.currentTimeMillis();
        Optional<Widget> w = repo.findById(id);
        long end = System.currentTimeMillis();
        long millis = end - ini;
        msg("find by Id operation duration (millis): " + millis);

        assertTrue(w.isPresent(), "widget with id " + id + " not found");
        /* this check is not supposed to be "exact", just to catch potential problems */
        assertTrue(millis < 3, "Get by Id execution time was higher than expected: " + millis);
        msg("widget with Id " + id + ": " + w.get());
    }

    private void testGetAllInternal() {
        msg("---- get all widgets --- Num of widgets: " + repo.size());
        long ini = System.currentTimeMillis();
        SortedSet<Widget> widgets = repo.findAll(null, null);
        long end = System.currentTimeMillis();
        msg("findAll operation duration (millis): " + (end - ini));
        int size = widgets.size();
        msg("number of widgets returned: " + size);
        assertEquals(repo.size(), size, "number of widgets returned by the getAll call should be the same as the number of widgets initially created");
        Widget first =  widgets.first();
        Widget last = widgets.last();
        assertEquals(0, first.getX());
        assertEquals(0, first.getY());
        assertEquals(1, first.getzIndex());
        assertEquals(size - 1, last.getX());
        assertEquals(size - 1, last.getY());
        assertEquals(size, last.getzIndex());
    }

    private void testGetPaginationInternal(int page_size, int page) {
        msg("---- get page: " + page + " with page_size: " + page_size + " --- Num of widgets: " + repo.size());
        long ini = System.currentTimeMillis();
        SortedSet<Widget> widgets = repo.findAll(page_size, page);
        long end = System.currentTimeMillis();
        msg("findAll (in page) operation duration (millis): " + (end - ini));
        int size = widgets.size();

        assertEquals(page_size, size, "expected number of widgets in a page");
        if(size > 0) {
            Widget first = widgets.first();
            Widget last = widgets.last();
            assertEquals((page -1) * page_size, first.getX());
            assertEquals((page -1) * page_size, first.getY());
            assertEquals(((page -1) * page_size) + 1, first.getzIndex());
            assertEquals(page * page_size -1, last.getX());
            assertEquals(page * page_size -1, last.getY());
            assertEquals(page * page_size, last.getzIndex());
        }
    }

    private void testGetAllWithinRectangleInternal(Point lowerLeft, Point upperRight) {
        SortedSet<Widget> widgets;
        long ini, end;
        long millis1, millis2;
        int size1, size2;
        Widget first1, first2;
        Widget last1, last2;

        msg("---- get all within rectangle (specialized API) --- Num of widgets: " + repo.size());
        ini = System.currentTimeMillis();
        widgets = repo.findAllInRectangle(lowerLeft, upperRight);
        end = System.currentTimeMillis();
        millis1 = end - ini;
        first1 = widgets.first();
        last1 = widgets.last();
        size1 = widgets.size();
        msg("Number of widgets returned: " + size1);
        msg("findAllWithinRectangle (specialized API) operation duration (millis): " + (end - ini));

        msg("---- get all within rectangle (generic api) --- Num of widgets: " + repo.size());
        ini = System.currentTimeMillis();
        widgets = repo.findAll(null, null, lowerLeft, upperRight);
        end = System.currentTimeMillis();
        millis2 = end - ini;
        first2 = widgets.first();
        last2 = widgets.last();
        size2 = widgets.size();
        msg("Number of widgets returned: " + size2);
        msg("findAllWithinRectangle (generic API) operation duration (millis): " + (end - ini));
        msg("------------------------");

        assertTrue(millis2 > millis1 * 10, "The dedicated API is expected to be at least 10 times faster");
        assertEquals(size1, size2);
        assertEquals(first1, first2);
        assertEquals(last1, last2);
    }

    @Test
    public void testGetById() {
        int size = 10000;
        fetchData(size);
        testGetByIdInternal(size);
    }

    @Test
    public void testGetApis1() {
        int size = 100000;
        fetchData(size);
        testGetByIdInternal(size);
        testGetAllInternal();
    }

    @Test
    public void testGetApis2() {
        int size = 1000000;
        fetchData(size);
        testGetByIdInternal(size);
        testGetAllInternal();
        testGetPaginationInternal(10, 10);
    }

    @Test
    public void testGetApis3() {
        int size = 10000000;
        fetchData(size);
        testGetByIdInternal(size);
        testGetPaginationInternal(100, 3100);
        testGetPaginationInternal(500, 15000);
        testGetPaginationInternal(100, 310);
        testGetAllInternal();
    }

    @Test
    public void testGetAllWithinRectangle() {
        int size = 10000000;
        fetchData(size);
        Point lowerLeft = Point.of(5000, 5500);
        Point upperRight = Point.of(6000, 6600);
        // do not fetch again, use data from previous tests
        testGetByIdInternal(size);
        // the dedicated API is expected to be much faster (it leverages the order by x coordinate)
        testGetAllWithinRectangleInternal(lowerLeft, upperRight);
    }
}
