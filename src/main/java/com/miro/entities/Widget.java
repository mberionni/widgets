package com.miro.entities;

import com.miro.WidgetRepository;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Widget implements Comparable<Widget> {
    private static final AtomicLong sequence = new AtomicLong();
    private Long id;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Integer zIndex;
    private LocalDateTime modificationDate;

    public Widget() {
    }

    public Widget(Integer x, Integer y, Integer width, Integer height, Integer zIndex) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zIndex = zIndex;
    }

    public static Widget of(Integer x, Integer y, Integer width, Integer height, Integer z_index, WidgetRepository repo) {
        Widget w = new Widget(x, y, width, height, z_index);
        w.validateAndComplete(repo);
        return w;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Integer getzIndex() {
        return zIndex;
    }

    public void setzIndex(Integer zIndex) {
        this.zIndex = zIndex;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Widget widget = (Widget) o;
        return zIndex.equals(widget.zIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zIndex);
    }

    public void merge(Widget widget, WidgetRepository repo) {
        setId(widget.getId());
        if (getX() == null) {
            setX(widget.getX());
        }
        if (getY() == null) {
            setY(widget.getY());
        }
        if (getWidth() == null) {
            setWidth(widget.getWidth());
        }
        if (getHeight() == null) {
            setHeight(widget.getHeight());
        }
        if (getzIndex() == null) {
            setzIndex(widget.getzIndex());
        }
        validateAndComplete(repo);
    }

    public void validateAndComplete(WidgetRepository repository) {
        if(x == null || y == null || width == null || height == null) {
            throw new InvalidParameterException("Widget's 'x', 'y', 'width' and 'height' parameters are mandatory.");
        }
        if(width <= 0) {
            throw new InvalidParameterException("Widget's 'width' must be greater than zero, but was: " + width + ".");
        }
        if(height <= 0) {
            throw new InvalidParameterException("Widget's 'height' must be greater than zero, but was: " + height + ".");
        }
        if(zIndex == null) {
            Widget max = repository.findMaxZIndex();
            zIndex = (max == null) ? 1 : max.getzIndex() + 1;
        }
        if(id == null) {
            id = sequence.incrementAndGet();
        }
        modificationDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Widget{" +
                "x=" + x +
                ", y=" + y +
                ", z_index=" + zIndex +
                ", width=" + width +
                ", height=" + height +
                ", id=" + id +
                '}';
    }

    @Override
    public int compareTo(Widget o) {
        if (this.zIndex.intValue() == o.zIndex.intValue()) {
            return 0;
        }
        return (this.zIndex > o.zIndex) ? 1 : -1;
    }
}