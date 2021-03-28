package com.miro;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class WidgetNotFoundAdvice {

    @ResponseBody
    @ExceptionHandler(WidgetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String widgetNotFoundHandler(WidgetNotFoundException ex) {
        return ex.getMessage();
    }
}