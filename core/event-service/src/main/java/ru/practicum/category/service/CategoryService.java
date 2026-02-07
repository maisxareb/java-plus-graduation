package ru.practicum.category.service;

import ru.practicum.category_service.dto.CategoryDto;
import ru.practicum.category_service.dto.CategoryRequestDto;

import java.util.List;

public interface CategoryService {

    CategoryDto add(CategoryRequestDto newDto);

    CategoryDto update(Long catId, CategoryRequestDto updDto);

    void delete(Long categoryId);

    CategoryDto getById(Long categoryId);

    List<CategoryDto> getAll(int from, int size);
}
