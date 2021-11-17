package com.miro.entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Widget implements Comparable<Widget> {
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
        return zIndex.intValue() == widget.zIndex.intValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(zIndex);
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