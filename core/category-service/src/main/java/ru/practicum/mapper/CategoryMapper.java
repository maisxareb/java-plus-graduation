package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequest;
import ru.practicum.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "categoryRequest.name")
    Category toCategory(CategoryRequest categoryRequest);

    @Mapping(target = "id", source = "categoryDto.id")
    @Mapping(target = "name", source = "categoryDto.name")
    Category toCategory(CategoryDto categoryDto);

    @Mapping(target = "id", source = "category.id")
    @Mapping(target = "name", source = "category.name")
    CategoryDto toCategoryDto(Category category);

    static Category toCategoryStatic(CategoryRequest categoryRequest) {
        if (categoryRequest == null) {
            return null;
        }
        Category category = new Category();
        category.setName(categoryRequest.getName());
        return category;
    }

    static Category toCategoryStatic(CategoryDto categoryDto) {
        if (categoryDto == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setName(categoryDto.getName());
        return category;
    }

    static CategoryDto toCategoryDtoStatic(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());
        return categoryDto;
    }
}
