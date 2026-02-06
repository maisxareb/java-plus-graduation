package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationControllerPublic {

    final CompilationService compilationService;
    static final String COMP_ID_PATH = "/{compId}";

    @Autowired
    public CompilationControllerPublic(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(name = "pinned", defaultValue = "false") boolean pinned,
                                                @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
                                                @RequestParam(name = "size", defaultValue = "10") @Positive int size) {
        return compilationService.getAllComps(pinned, from, size);
    }

    @GetMapping(COMP_ID_PATH)
    public CompilationDto getCompilations(@PathVariable int compId) {
        return compilationService.getCompById(compId);
    }
}
