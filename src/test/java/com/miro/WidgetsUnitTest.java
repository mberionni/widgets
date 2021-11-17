package com.miro;

import com.miro.entities.Point;
import com.miro.entities.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetsUnitTest {

    private static final WidgetRepository repo = new WidgetMainRepository();
    private static final WidgetUtil util = new WidgetUtil();

    @BeforeEach
    void clear() {
        repo.clear();
    }

    @Test
    void testCreateWidget() {
        Widget w = util.of(20, 20, 50, 50, null);
        repo.save(w);

        /*
        ** Returns an <a href="#unmodifiable">unmodifiable List</a> containing the elements of
        ** the given Collection, in its iteration order. The given Collection must not be null,
        ** and it must not contain any null elements. If the given Collection is subsequently
        ** modified, the returned List will not reflect such modifications.
        */
        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(1, widgets.size());

        Widget widget = widgets.get(0);
        assertEquals(1, widget.getzIndex());
        assertEquals(20, widget.getX());
    }

    @Test
    void testZIndexShiftAll() {
        Widget w1 = util.of(10, 10, 50, 50, 1);
        repo.save(w1);
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, 3);
        repo.save(w3);
        long id3 = w3.getId();
        Widget w4 = util.of(40, 40, 50, 50, 4);
        repo.save(w4);
        long id4 = w4.getId();

        Widget wx = repo.findById(id3).get();
        assertEquals(3, wx.getzIndex());

        /* add a widget with an already existing z-index */
        Widget w5 = util.of(50, 50, 50, 50, 2);
        repo.save(w5);
        long id5 = w5.getId();

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(5, widgets.size());

        Widget widget = widgets.get(0);
        assertEquals(1, widget.getzIndex());
        assertEquals(10, widget.getX());

        widget = widgets.get(1);
        assertEquals(2, widget.getzIndex());
        assertEquals(id5, widget.getId());
        assertEquals(50, widget.getX());

        widget = widgets.get(2);
        assertEquals(3, widget.getzIndex());
        assertEquals(20, widget.getX());

        widget = widgets.get(3);
        assertEquals(4, widget.getzIndex());
        assertEquals(id3, widget.getId());
        assertEquals(30, widget.getX());

        wx = repo.findById(id3).get();
        assertEquals(4, wx.getzIndex());
        assertEquals(30, wx.getX());

        wx = repo.findById(id4).get();
        assertEquals(5, wx.getzIndex());
        assertEquals(40, wx.getX());
    }

    @Test
    void testZIndexShiftSingle() {
        Widget w1 = util.of(10, 10, 50, 50, 1);
        repo.save(w1);
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, 4);
        repo.save(w3);
        long id3 = w3.getId();
        Widget w4 = util.of(40, 40, 50, 50, 5);
        repo.save(w4);
        long id4 = w4.getId();

        /* add a widget with an already existing z-index */
        Widget w5 = util.of(50, 50, 50, 50, 2);
        repo.save(w5);
        long id5 = w5.getId();

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(5, widgets.size());

        Widget widget = widgets.get(0);
        assertEquals(1, widget.getzIndex());
        assertEquals(10, widget.getX());

        widget = widgets.get(1);
        assertEquals(2, widget.getzIndex());
        assertEquals(id5, widget.getId());
        assertEquals(50, widget.getX());

        widget = widgets.get(2);
        assertEquals(3, widget.getzIndex());
        assertEquals(20, widget.getX());

        widget = widgets.get(3);
        assertEquals(4, widget.getzIndex());
        assertEquals(id3, widget.getId());
        assertEquals(30, widget.getX());

        widget = widgets.get(4);
        assertEquals(5, widget.getzIndex());
        assertEquals(id4, widget.getId());
        assertEquals(40, widget.getX());
    }

    @Test
    void testZIndexNoShift() {
        Widget w1 = util.of(10, 10, 50, 50, 1);
        repo.save(w1);
        Widget w2 = util.of(20, 20, 50, 50, 3);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, 4);
        repo.save(w3);

        /* add a widget with a z-index that fills a gap */
        Widget w4 = util.of(40, 40, 50, 50, 2);
        repo.save(w4);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(4, widgets.size());

        Widget widget = widgets.get(0);
        assertEquals(1, widget.getzIndex());
        assertEquals(10, widget.getX());

        widget = widgets.get(1);
        assertEquals(2, widget.getzIndex());
        assertEquals(40, widget.getX());

        widget = widgets.get(2);
        assertEquals(3, widget.getzIndex());
        assertEquals(20, widget.getX());

        widget = widgets.get(3);
        assertEquals(4, widget.getzIndex());
        assertEquals(30, widget.getX());
    }

    @Test
    void testZIndexNoShiftWithGap() {
        Widget w1 = util.of(10, 10, 50, 50, 1);
        repo.save(w1);
        Widget w2 = util.of(20, 20, 50, 50, 5);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, 6);
        repo.save(w3);

        /* add a widget with a z-index in a gap */
        Widget w4 = util.of(40, 40, 50, 50, 2);
        repo.save(w4);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(4, widgets.size());

        Widget widget = widgets.get(0);
        assertEquals(1, widget.getzIndex());
        assertEquals(10, widget.getX());

        widget = widgets.get(1);
        assertEquals(2, widget.getzIndex());
        assertEquals(40, widget.getX());

        widget = widgets.get(2);
        assertEquals(5, widget.getzIndex());
        assertEquals(20, widget.getX());

        widget = widgets.get(3);
        assertEquals(6, widget.getzIndex());
        assertEquals(30, widget.getX());
    }

    @Test
    void testUpdateWidget() {
        Widget w1 = util.of(10, 10, 100, 100, 1);
        repo.save(w1);
        long id1 = w1.getId();
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        long id2 = w2.getId();

        Widget newWidget = new Widget(null, 33, null, 56, null);
        util.merge(w1, newWidget); // assign to newWidget the same zIndex and id of w1
        repo.update(w1, newWidget);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(2, widgets.size());

        w1 = widgets.get(0);
        assertEquals(10, w1.getX());
        assertEquals(33, w1.getY());
        assertEquals(100, w1.getWidth());
        assertEquals(56, w1.getHeight());
        assertEquals(id1, w1.getId());
        assertEquals(1, w1.getzIndex());

        w2 = widgets.get(1);
        assertEquals(20, w2.getX());
        assertEquals(20, w2.getY());
        assertEquals(50, w2.getWidth());
        assertEquals(50, w2.getHeight());
        assertEquals(id2, w2.getId());
        assertEquals(2, w2.getzIndex());
    }

    @Test
    void testUpdateWidgetWithZIndex() {
        Widget w1 = util.of(10, 10, 40, 40, 1);
        repo.save(w1);
        long id1 = w1.getId();
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        long id2 = w2.getId();

        Widget widgetNew = new Widget(33, 23, null, null, 1);
        util.merge(w1, widgetNew);
        repo.update(w1, widgetNew);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(2, widgets.size());

        w1 = widgets.get(0);
        assertEquals(33, w1.getX());
        assertEquals(23, w1.getY());
        assertEquals(40, w1.getWidth());
        assertEquals(40, w1.getHeight());
        assertEquals(id1, w1.getId());
        assertEquals(1, w1.getzIndex());

        w2 = widgets.get(1);
        assertEquals(20, w2.getX());
        assertEquals(20, w2.getY());
        assertEquals(50, w2.getWidth());
        assertEquals(50, w2.getHeight());
        assertEquals(id2, w2.getId());
        assertEquals(2, w2.getzIndex());
    }

    @Test
    void testUpdateWidgetWithZIndexShift() {
        Widget w1 = util.of(10, 10, 40, 40, 1);
        repo.save(w1);
        long id1 = w1.getId();
        Widget w2 = util.of(20, 20, 50, 50, 4);
        repo.save(w2);
        long id2 = w2.getId();
        Widget w3 = util.of(30, 30, 60, 60, 5);
        repo.save(w3);
        long id3 = w3.getId();
        Widget w4 = util.of(40, 40, 70, 70, 7);
        repo.save(w4);
        long id4 = w4.getId();

        Widget widgetNew = new Widget(33, 23, null, null, 5);
        util.merge(w2, widgetNew);
        repo.update(w2, widgetNew);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        assertEquals(4, widgets.size());

        w1 = widgets.get(0);
        assertEquals(10, w1.getX());
        assertEquals(10, w1.getY());
        assertEquals(40, w1.getWidth());
        assertEquals(40, w1.getHeight());
        assertEquals(id1, w1.getId());
        assertEquals(1, w1.getzIndex());

        w2 = widgets.get(1);
        assertEquals(33, w2.getX());
        assertEquals(23, w2.getY());
        assertEquals(50, w2.getWidth());
        assertEquals(50, w2.getHeight());
        assertEquals(id2, w2.getId());
        assertEquals(5, w2.getzIndex());

        w3 = widgets.get(2);
        assertEquals(30, w3.getX());
        assertEquals(30, w3.getY());
        assertEquals(60, w3.getWidth());
        assertEquals(60, w3.getHeight());
        assertEquals(id3, w3.getId());
        assertEquals(6, w3.getzIndex());

        w4 = widgets.get(3);
        assertEquals(40, w4.getX());
        assertEquals(40, w4.getY());
        assertEquals(70, w4.getWidth());
        assertEquals(70, w4.getHeight());
        assertEquals(id4, w4.getId());
        assertEquals(7, w4.getzIndex());
    }

    @Test
    void testDeletingWidget() {
        Widget w1 = util.of(10, 10, 50, 50, 1);
        repo.save(w1);
        long id1 = w1.getId();
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        long id2 = w2.getId();
        Widget w3 = util.of(30, 30, 50, 50, 3);
        repo.save(w3);
        long id3 = w3.getId();

        assertEquals(3, repo.size());

        repo.deleteById(id2);
        assertEquals(2, repo.size());

        assertTrue(repo.findById(id2).isEmpty());
        assertTrue(repo.findById(id1).isPresent());
        assertTrue(repo.findById(id3).isPresent());
    }

    @Test
    void testGetAllWidgets() {
        /* widgets are returned sorted by z-index */
        Widget w1 = util.of(10, 10, 50, 50, 10);
        repo.save(w1);
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, 40);
        repo.save(w3);
        Widget w4 = util.of(40, 40, 50, 50, 4);
        repo.save(w4);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        w1 = widgets.get(0);
        assertEquals(2, w1.getzIndex());
        w1 = widgets.get(1);
        assertEquals(4, w1.getzIndex());
        w1 = widgets.get(2);
        assertEquals(10, w1.getzIndex());
        w1 = widgets.get(3);
        assertEquals(40, w1.getzIndex());
    }

    @Test
    void testGetAllWidgetsWithNegativeZIndex() {
        /* widgets are returned sorted by z-index */
        Widget w1 = util.of(-10, 10, 50, 50, -10);
        repo.save(w1);
        System.out.println("w1 id:" + w1.getId());
        Widget w2 = util.of(20, 20, 50, 50, 2);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, -40);
        repo.save(w3);
        Widget w4 = util.of(-4, 40, 50, 50, 4);
        repo.save(w4);

        List<Widget> widgets = List.copyOf(repo.findAll(null, null));
        w1 = widgets.get(0);
        assertEquals(-40, w1.getzIndex());
        assertEquals(30, w1.getX());
        w1 = widgets.get(1);
        assertEquals(-10, w1.getzIndex());
        assertEquals(-10, w1.getX());
        w1 = widgets.get(2);
        assertEquals(2, w1.getzIndex());
        assertEquals(20, w1.getX());
        w1 = widgets.get(3);
        assertEquals(4, w1.getzIndex());
        assertEquals(-4, w1.getX());
    }

    @Test
    void testGetAllWidgetsWithPagination() {
        /* widgets are returned sorted by z-index */
        Widget w1 = util.of(10, 10, 50, 50, 10);
        repo.save(w1);
        Widget w2 = util.of(20, 20, 50, 50, 1);
        repo.save(w2);
        Widget w3 = util.of(30, 30, 50, 50, 40);
        repo.save(w3);
        Widget w4 = util.of(40, 40, 50, 50, 4);
        repo.save(w4);
        Widget w5 = util.of(50, 50, 50, 50, 3);
        repo.save(w5);
        Widget w6 = util.of(60, 60, 50, 50, 5);
        repo.save(w6);
        Widget w7 = util.of(70, 70, 50, 50, 6);
        repo.save(w7);
        Widget w8 = util.of(80, 80, 50, 50, 7);
        repo.save(w8);
        Widget w9 = util.of(90, 90, 50, 50, 8);
        repo.save(w9);

        List<Widget> widgets = List.copyOf(repo.findAll(4, 1));
        assertEquals(4, widgets.size());
        w1 = widgets.get(0);
        assertEquals(1, w1.getzIndex());
        assertEquals(20, w1.getX());
        w1 = widgets.get(1);
        assertEquals(3, w1.getzIndex());
        assertEquals(50, w1.getX());
        w1 = widgets.get(2);
        assertEquals(4, w1.getzIndex());
        assertEquals(40, w1.getX());
        w1 = widgets.get(3);
        assertEquals(5, w1.getzIndex());
        assertEquals(60, w1.getX());

        widgets = List.copyOf(repo.findAll(4, 2));
        assertEquals(4, widgets.size());
        w1 = widgets.get(0);
        assertEquals(6, w1.getzIndex());
        assertEquals(70, w1.getX());
        w1 = widgets.get(1);
        assertEquals(7, w1.getzIndex());
        assertEquals(80, w1.getX());
        w1 = widgets.get(2);
        assertEquals(8, w1.getzIndex());
        assertEquals(90, w1.getX());
        w1 = widgets.get(3);
        assertEquals(10, w1.getzIndex());
        assertEquals(10, w1.getX());

        widgets = List.copyOf(repo.findAll(4, 3));
        assertEquals(1, widgets.size());
        w1 = widgets.get(0);
        assertEquals(40, w1.getzIndex());
        assertEquals(30, w1.getX());
    }

    @Test
    void testGetAllWidgetsWithinRectangle() {
        Widget w1 = util.of(0, 0, 100, 100, 10);
        repo.save(w1);
        Widget w2 = util.of(0, 50, 100, 100, 1);
        repo.save(w2);
        Widget w3 = util.of(50, 50, 100, 100, 40);
        repo.save(w3);
        Widget w4 = util.of(60, 60, 100, 100, 4);
        repo.save(w4);

        Point lowerLeft = Point.of(0, 0);
        Point upperRight = Point.of(100, 150);
        List<Widget> widgets = List.copyOf(repo.findAllInRectangle(lowerLeft, upperRight));
        assertEquals(2, widgets.size());

        w1 = widgets.get(0);
        assertEquals(1, w1.getzIndex());
        assertEquals(0, w1.getX());
        assertEquals(50, w1.getY());

        w2 = widgets.get(1);
        assertEquals(10, w2.getzIndex());
        assertEquals(0, w2.getX());
        assertEquals(0, w2.getY());
    }
}