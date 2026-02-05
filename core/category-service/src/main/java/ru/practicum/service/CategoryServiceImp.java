package ru.practicum.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequest;
import ru.practicum.event.client.EventClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ConstraintException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Category;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.repository.CategoryRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImp implements CategoryService {

    CategoryRepository repository;
    EventClient eventClient;
    CategoryMapper categoryMapper;
    String errorMessageNotFound = "Категория с id = %d не найдена";
    String errorMessageAlreadyExist = "Категория с именем = %s уже существует";

    @Override
    public List<CategoryDto> getAll(Integer from, Integer size) {
        PageRequest page = PageRequest.of(from, size, Sort.by("id").ascending());
        List<Category> categories = repository.findAll(page).getContent();
        return categories.stream()
                .map(categoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getById(Long categoryId) {
        Category category = repository.findById(categoryId).orElseThrow(() ->
                new NotFoundException(String.format(errorMessageNotFound, categoryId))
        );
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    public CategoryDto create(CategoryRequest categoryRequest) {
        if (repository.findAll().stream().map(Category::getName).anyMatch(name -> name.equals(categoryRequest.getName()))) {
            throw new ConstraintException(String.format(errorMessageAlreadyExist, categoryRequest.getName()));
        }
        Category category = categoryMapper.toCategory(categoryRequest);
        Category savedCategory = repository.save(category);
        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public CategoryDto update(Long categoryId, CategoryRequest categoryRequest) {
        Category category = repository.findById(categoryId).orElseThrow(() ->
                new NotFoundException(String.format(errorMessageNotFound, categoryId))
        );
        Optional<Category> existingCategory = repository.findByName(categoryRequest.getName());
        if (existingCategory.isPresent() && !existingCategory.get().getId().equals(categoryId)) {
            throw new ConflictException(String.format(errorMessageAlreadyExist, categoryRequest.getName()));
        }
        category.setName(categoryRequest.getName());
        Category updatedCategory = repository.save(category);
        return categoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    public void delete(Long categoryId) {
        Category category = repository.findById(categoryId).orElseThrow(() ->
                new NotFoundException(String.format(errorMessageNotFound, categoryId))
        );
        if (eventClient.existsByCategory(category.getId())) {
            throw new ConflictException("Невозможно удалить категорию: существуют события, связанные с этой категорией");
        }
        repository.deleteById(categoryId);
    }

    @Override
    public Map<Long, CategoryDto> getMap(List<Long> ids) {
        List<Category> categories = repository.findAllByIdIn(ids);
        return categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        categoryMapper::toCategoryDto
                ));
    }
}
