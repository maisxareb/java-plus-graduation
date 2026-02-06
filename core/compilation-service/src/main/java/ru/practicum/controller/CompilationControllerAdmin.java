package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.service.CompilationService;

@RestController
@RequestMapping("/admin/compilations")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationControllerAdmin {
    final CompilationService compilationService;
    static final String COMP_ID_PATH = "/{compId}";

    public CompilationControllerAdmin(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto postCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        return compilationService.create(newCompilationDto);
    }

    @DeleteMapping(COMP_ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable(name = "compId") int compId) {
        compilationService.deleteById(compId);
    }

    @PatchMapping(COMP_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto patchCompilation(@PathVariable(name = "compId") int compId,
                                           @Valid @RequestBody UpdateCompilationRequest updateRequest) {
        return compilationService.update(compId, updateRequest);
    }
}
