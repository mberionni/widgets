package com.miro;

import com.miro.entities.Widget;

import java.security.InvalidParameterException;

public class WidgetUtil {

    public void merge(Widget widget, Widget newWidget) {
        newWidget.setId(widget.getId());
        if (newWidget.getX() == null) {
            newWidget.setX(widget.getX());
        }
        if (newWidget.getY() == null) {
            newWidget.setY(widget.getY());
        }
        if (newWidget.getWidth() == null) {
            newWidget.setWidth(widget.getWidth());
        }
        if (newWidget.getHeight() == null) {
            newWidget.setHeight(widget.getHeight());
        }
        if (newWidget.getzIndex() == null) {
            newWidget.setzIndex(widget.getzIndex());
        }
        validate(newWidget);
    }

    public void validate(Widget widget) {
        if(widget.getX() == null || widget.getY() == null || widget.getWidth() == null || widget.getHeight() == null) {
            throw new InvalidParameterException("Widget's 'x', 'y', 'width' and 'height' parameters are mandatory.");
        }
        if(widget.getWidth() <= 0) {
            throw new InvalidParameterException("Widget's 'width' must be greater than zero, but was: " + widget.getWidth() + ".");
        }
        if(widget.getHeight() <= 0) {
            throw new InvalidParameterException("Widget's 'height' must be greater than zero, but was: " + widget.getHeight() + ".");
        }
    }

    public Widget of(Integer x, Integer y, Integer width, Integer height, Integer z_index) {
        Widget w = new Widget(x, y, width, height, z_index);
        validate(w);
        return w;
    }
}
