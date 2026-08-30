package com.illbethere.web;

import com.illbethere.domain.AppUser;
import com.illbethere.service.PromiseService;
import com.illbethere.web.dto.PromiseDtos.CreatePromiseRequest;
import com.illbethere.web.dto.PromiseDtos.PromiseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/promises")
public class PromiseController {

    private final PromiseService promiseService;

    public PromiseController(PromiseService promiseService) {
        this.promiseService = promiseService;
    }

    @PostMapping
    public PromiseResponse create(@Valid @RequestBody CreatePromiseRequest request) {
        return promiseService.create(request, CurrentUser.require());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        promiseService.cancel(id, CurrentUser.require());
    }

    @GetMapping("/mine")
    public List<PromiseResponse> mine() {
        AppUser user = CurrentUser.require();
        return promiseService.myPromises(user);
    }
}
