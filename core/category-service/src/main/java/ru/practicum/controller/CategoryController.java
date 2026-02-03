package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequest;
import ru.practicum.service.CategoryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryController {

    final CategoryService service;
    static final String ADMIN_PATH = "/admin/categories";
    static final String PUBLIC_PROTECTED_PATH = "/categories";
    static final String ID_PATH = "/{catId}";
    static final String FEIGN_CLIENT_PATH = "/feign/categories";

    @GetMapping(PUBLIC_PROTECTED_PATH)
    public List<CategoryDto> getAll(@RequestParam(defaultValue = "0") Integer from, @RequestParam(defaultValue = "10") Integer size) {
        return service.getAll(from, size);
    }

    @GetMapping(PUBLIC_PROTECTED_PATH + ID_PATH)
    public CategoryDto getById(@PathVariable(name = "catId") Long categoryId) {
        return service.getById(categoryId);
    }

    @PostMapping(ADMIN_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@RequestBody @Valid CategoryRequest categoryRequest) {
        return service.create(categoryRequest);
    }

    @PatchMapping(ADMIN_PATH + ID_PATH)
    public CategoryDto update(@RequestBody @Valid CategoryRequest categoryRequest, @PathVariable(name = "catId") Long categoryId) {
        return service.update(categoryId, categoryRequest);
    }

    @DeleteMapping(ADMIN_PATH + ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "catId") Long categoryId) {
        service.delete(categoryId);
    }

    @GetMapping(FEIGN_CLIENT_PATH + ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto getCategoryForFeign(@PathVariable(name = "catId") Long categoryId) {
        return service.getById(categoryId);
    }

    @GetMapping(FEIGN_CLIENT_PATH)
    @ResponseStatus(HttpStatus.OK)
    public Map<Long, CategoryDto> getMapForFeign(@RequestParam(name = "ids") List<Long> ids) {
        return service.getMap(ids);
    }
}
