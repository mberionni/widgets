package com.miro;

public class WidgetNotFoundException extends RuntimeException {

    WidgetNotFoundException(Long id) {
        super("Widget with id '" + id + "' not found!");
    }
}
