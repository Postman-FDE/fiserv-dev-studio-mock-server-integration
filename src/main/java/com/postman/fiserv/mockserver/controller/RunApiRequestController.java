package com.postman.fiserv.mockserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.postman.fiserv.mockserver.model.RunApiRequest;
import com.postman.fiserv.mockserver.service.RunApiRequestService;

@RestController
public class RunApiRequestController {

    private final RunApiRequestService service;

    public RunApiRequestController(RunApiRequestService service) {
        this.service = service;
    }

    @PostMapping("/api-run")
    public ResponseEntity<byte[]> run(@RequestBody RunApiRequest request) {
        return service.run(request);
    }
}
