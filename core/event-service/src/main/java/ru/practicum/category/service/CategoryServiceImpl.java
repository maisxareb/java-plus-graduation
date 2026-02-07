package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category_service.dto.CategoryDto;
import ru.practicum.category_service.dto.CategoryRequestDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.user_service.exception.ConflictException;
import ru.practicum.user_service.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Все методы по умолчанию только для чтения
public class CategoryServiceImpl implements CategoryService {

    // Репозитории для доступа к данным
    private final EventRepository eventRepository; // Для проверки связанных событий
    private final CategoryRepository categoryRepository; // Для работы с категориями

    private final CategoryMapper categoryMapper; // Маппер для преобразования DTO <-> Entity

    /**
     * Создание новой категории
     */
    @Override
    @Transactional // Переопределяем аннотацию для методов записи
    public CategoryDto add(CategoryRequestDto newDto) {
        log.debug("Метод add(); categoryRequestDto: {}", newDto);

        // Проверяем уникальность имени категории
        this.validateCategoryNameExists(newDto.getName());

        // Создаем и сохраняем категорию
        Category category = categoryMapper.toEntity(newDto);
        category.setName(newDto.getName()); // Явное установление имени (может быть избыточно)
        category = categoryRepository.save(category);

        return categoryMapper.toDto(category);
    }

    /**
     * Обновление существующей категории
     */
    @Override
    @Transactional
    public CategoryDto update(Long categoryId, CategoryRequestDto updDto) {
        log.debug("Метод update(); categoryId: {}, dto: {}", categoryId, updDto);

        // Проверяем уникальность имени, исключая текущую категорию
        this.validateCategoryNameExists(updDto.getName(), categoryId);

        // Находим и обновляем категорию
        Category category = this.findCategoryById(categoryId);
        category.setName(updDto.getName());
        category = categoryRepository.save(category);

        return categoryMapper.toDto(category);
    }

    /**
     * Удаление категории
     * Проверяет, не используется ли категория в событиях
     */
    @Override
    @Transactional
    public void delete(Long categoryId) {
        log.debug("Метод delete(); categoryId: {}", categoryId);

        // Проверяем существование категории
        this.validateCategoryExists(categoryId);

        // Проверяем, нет ли событий с этой категорией
        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Category с id={} используется", categoryId);
        }

        // Удаляем категорию
        categoryRepository.deleteById(categoryId);
    }

    /**
     * Получение категории по ID
     */
    @Override
    public CategoryDto getById(Long categoryId) {
        log.debug("Метод getById(); categoryId: {}", categoryId);

        Category category = this.findCategoryById(categoryId);
        return categoryMapper.toDto(category);
    }

    /**
     * Получение списка категорий с пагинацией
     * from - начальный индекс (смещение)
     * size - количество элементов на странице
     */
    @Override
    public List<CategoryDto> getAll(int from, int size) {
        log.debug("Метод getAll(); from: {}, size: {}", from, size);

        // Рассчитываем номер страницы для пагинации
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        // Получаем данные с пагинацией
        List<Category> categories = categoryRepository.findAll(pageable).getContent();

        // Преобразуем в DTO
        return categories.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Вспомогательный метод: проверка существования имени категории (без учета регистра)
     * Используется при создании новой категории
     */
    private void validateCategoryNameExists(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category name={} уже существует", name);
        }
    }

    /**
     * Вспомогательный метод: проверка уникальности имени при обновлении
     * Проверяет, существует ли категория с таким именем, исключая категорию с указанным ID
     */
    private void validateCategoryNameExists(String name, Long categoryId) {
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw new ConflictException("Category name={} уже существует", name);
        }
    }

    /**
     * Вспомогательный метод: проверка существования категории по ID
     */
    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category id={} не найдена", categoryId);
        }
    }

    /**
     * Вспомогательный метод: поиск категории по ID с обработкой исключения
     * Возвращает Category или выбрасывает NotFoundException
     */
    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category id={} не найдена", categoryId));
    }
}