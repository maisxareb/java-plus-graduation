package ru.practicum.service;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequest;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    List<CategoryDto> getAll(Integer from, Integer size);

    CategoryDto getById(Long categoryId);

    CategoryDto create(CategoryRequest categoryRequest);

    CategoryDto update(Long categoryId, CategoryRequest categoryRequest);

    void delete(Long categoryId);

    Map<Long, CategoryDto> getMap(List<Long> ids);
}
