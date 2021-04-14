package com.miro;

import com.miro.entities.Point;
import com.miro.entities.Widget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.SortedSet;

@RestController
public class WidgetController {

    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    WidgetRepository repository;

    WidgetController(WidgetRepository repository) {
        this.repository = repository;
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from the Widget Controller!";
    }

    /* pass the state because we do not want to keep the state */
    @GetMapping("/widgets")
    public ResponseEntity<SortedSet<Widget>> getAllWidgets(@RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Point lowerLeft,
            @RequestParam(required = false) Point upperRight) {
        SortedSet<Widget> widgets;

        if (size == null && page == null && lowerLeft != null && upperRight != null) {
            widgets = repository.findAllInRectangle(lowerLeft, upperRight);
        } else {
            int page_size;
            if (size == null) {
                page_size = 10;
            } else {
                if (size <= 0) {
                    throw new InvalidParameterException("Page size must be greater than zero, but was: " + size + ".");
                }
                if (size > MAX_PAGE_SIZE) {
                    throw new InvalidParameterException("Page size can be at most " + MAX_PAGE_SIZE + ", but was: " + size + ".");
                }
                page_size = size;
            }
            if (page != null && page <= 0) {
                throw new InvalidParameterException("The requested 'page' must be greater than zero, but was: " + page + ".");
            }
            widgets = repository.findAll(page_size, page, lowerLeft, upperRight);
        }
        return ResponseEntity.ok().body(widgets);
    }

    @GetMapping("/widgets/{id}")
    ResponseEntity<Widget> getWidget(@PathVariable long id) {
        Optional<Widget> widget = repository.findById(id);
        if(widget.isEmpty()) {
            throw new WidgetNotFoundException(id);
        }
        return ResponseEntity.ok().body(widget.get());
    }

    @PostMapping("/widgets")
    ResponseEntity<Widget> createWidget(@RequestBody Widget newWidget)  {
        newWidget.validateAndComplete(repository);
        repository.save(newWidget);
        URI uri = URI.create("/widgets/" + newWidget.getId());
        return ResponseEntity.created(uri).body(newWidget);
    }

    @PutMapping("/widgets/{id}")
    ResponseEntity<Widget> updateWidget(@RequestBody Widget newWidget, @PathVariable long id) {
        Optional<Widget> widgetOpt = repository.findById(id);
        if(widgetOpt.isEmpty()) {
            throw new WidgetNotFoundException(id);
        }
        Widget widget = widgetOpt.get();
        newWidget.merge(widget, repository);
        repository.update(widget, newWidget);
        return ResponseEntity.ok().body(newWidget);
    }

    @DeleteMapping("/widgets/{id}")
    ResponseEntity<?> deleteWidget(@PathVariable long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
