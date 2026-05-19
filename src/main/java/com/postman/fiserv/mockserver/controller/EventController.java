package com.postman.fiserv.mockserver.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.postman.fiserv.mockserver.model.EventServiceRequest;
import com.postman.fiserv.mockserver.model.EventServiceResponse;
import com.postman.fiserv.mockserver.service.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public EventServiceResponse handle(@Valid @RequestBody EventServiceRequest request) {
        return eventService.process(request);
    }
}
