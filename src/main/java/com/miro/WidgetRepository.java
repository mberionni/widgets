package com.miro;

import com.miro.entities.Point;
import com.miro.entities.Widget;

import java.util.Optional;
import java.util.SortedSet;

public interface WidgetRepository  {

    void save(Widget widget);
    void update(Widget widget, Widget newWidget);
    Optional<Widget> findById(long id);
    SortedSet<Widget> findAll(Integer size, Integer page);
    SortedSet<Widget> findAllInRectangle(Point lowerLeft, Point upperRight);
    SortedSet<Widget> findAll(Integer size, Integer page, Point lowerLeft, Point upperRight);
    void deleteById(long id);
    Widget findMaxZIndex();
    int size();
    void clear();
}
