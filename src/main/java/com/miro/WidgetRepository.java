package com.miro;

import com.miro.entities.Widget;

import java.util.List;
import java.util.Optional;

public interface WidgetRepository  {

    void save(Widget widget);
    void update(Widget widget, Widget newWidget);
    Optional<Widget> findById(long id);
    List<Widget> findAll(Integer size, Integer page);
    void deleteById(long id);
    Widget findMaxZIndex();
    int size();
    void clear();
}
