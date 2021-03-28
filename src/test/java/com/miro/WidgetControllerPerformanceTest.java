package com.miro;

import com.miro.entities.Widget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetControllerPerformanceTest {

    private static final WidgetRepository repo = new WidgetMainRepository();

    private void fetchData(int size) {
        for (int i = 0; i < size; i++) {
            Widget w = Widget.of(i, i, 10, 10, null, repo);
            repo.save(w);
        }
        msg("created " + size + " widgets");
    }

    private void testGetByIdInternal(int size) {
        msg("--------get by Id ------------");
        int id = ThreadLocalRandom.current().nextInt(0, size + 1);
        fetchData(size);
        msg("fetched " + size + " widgets. Total number of widgets: " + repo.size());

        long ini = System.currentTimeMillis();
        Optional<Widget> w = repo.findById(id);
        long end = System.currentTimeMillis();
        msg("search operation duration (millis): " + (end - ini));

        /* this check is not supposed to be "exact", just to catch potential problems */
        assertTrue((end - ini) < 3);
        if (w.isEmpty()) {
            msg("widget with id " + id + " not found");
        } else {
            msg("widget with Id " + id + ": " + w.get());
        }
    }

    private void testGetAllInternal() {
        msg("---- get all widgets -------------");
        long ini = System.currentTimeMillis();
        List<Widget> widgets = repo.findAll(null, null);
        long end = System.currentTimeMillis();
        msg("search operation duration (millis): " + (end - ini));
        int size = widgets.size();
        msg("number of widgets returned: " + size);
        msg("first widget: " + widgets.get(0));
        msg("last widget: " + widgets.get(size-1));
        msg("------------------------");
    }

    private void testGetPaginationInternal(int page_size, int page) {
        msg("---- get page ----------------");
        long ini = System.currentTimeMillis();
        List<Widget> widgets = repo.findAll(page_size, page);
        long end = System.currentTimeMillis();
        msg("search operation duration (millis): " + (end - ini));
        int size = widgets.size();
        msg("number of widgets returned: " + size);
        msg("first widget: " + widgets.get(0));
        msg("last widget: " + widgets.get(size-1));
        msg("------------------------");
    }

    @Test
    public void testGetById1() {
        testGetByIdInternal(10000);
    }

    @Test
    public void testGetById2() {
        testGetByIdInternal(100000);
        testGetAllInternal();
    }

    @Test
    public void testGetById3() {
        testGetByIdInternal(1000000);
        testGetAllInternal();
        testGetPaginationInternal(10, 10);
    }

    @Test
    public void testGetById4() {
        testGetByIdInternal(10000000);
        testGetAllInternal();
        testGetPaginationInternal(100, 3100);
    }

    private static void msg(String m) {
        System.out.println(m);
    }
}
