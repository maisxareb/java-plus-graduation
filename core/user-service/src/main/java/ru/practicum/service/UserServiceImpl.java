package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user_service.dto.NewUserRequest;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.exception.BadRequestException;
import ru.practicum.user_service.exception.ConflictException;
import ru.practicum.user_service.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Все методы по умолчанию только для чтения
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;       // Маппер для преобразования между DTO и Entity
    private final UserRepository userRepository; // Репозиторий для работы с пользователями в БД

    //Создание нового пользователя
    @Override
    @Transactional //Переопределяем для метода записи
    public UserDto add(NewUserRequest newDto) {
        log.debug("Метод add(); userInputDto={}", newDto);

        //Проверка уникальности email
        if (userRepository.existsByEmail(newDto.getEmail())) {
            throw new ConflictException("User с Email={} уже существует", newDto.getEmail());
        }

        // Проверка что часть email до @ не превышает 64 символа
        String localpart = newDto.getEmail().substring(0, newDto.getEmail().indexOf('@'));
        if (localpart.length() > 64) {
            throw new BadRequestException("Localpart is too long");
        }

        // 3. Преобразование DTO в Entity и сохранение
        User savedUser = userRepository.save(userMapper.toEntity(newDto));

        log.debug("Метод add(); User создан savedUser={}", newDto); // Логирование DTO вместо Entity

        // 4. Преобразование обратно в DTO для возврата
        return userMapper.toFullDto(savedUser);
    }

    //Получение списка пользователей с фильтрацией и пагинацией
    @Override
    public List<UserDto> findAllBy(List<Long> ids, Integer from, Integer size) {
        log.debug("Метод findAll(); ids={}, from={}, size={}", ids, from, size);

        // Рассчет номера страницы для пагинации
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        List<User> users;

        // Выбор стратегии поиска в зависимости от наличия фильтра по ID
        if (ids == null || ids.isEmpty()) {
            //Получение всех пользователей с пагинацией
            users = userRepository.findAll(pageable).getContent();
        } else {
            //Получение только указанных пользователей с пагинацией
            users = userRepository.findAllByIdIn(ids, pageable);
        }

        // Преобразование в DTO
        return users.stream()
                .map(userMapper::toFullDto)
                .toList();
    }

    //Удаление пользователя по id
    @Override
    @Transactional
    public void delete(Long userId) {
        log.debug("Сервис UserServiceImpl; Метод delete(); userId={}", userId);

        // Проверка существования пользователя перед удалением
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new NotFoundException("User userId={} не найден", userId);
        }
    }

    //Получение пользователя по id
    @Override
    public UserDto getUserById(Long userId) {
        // Поиск пользователя с обработкой случая "не найден"
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User userId={} не найден", userId));
        return userMapper.toFullDto(user);
    }

    //Получение списка пользователей по списку id
    @Override
    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.info("Получение пользователей по IDs: {}", ids);

        //Обработка пустого или null списка
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        //Получение всех пользователей по списку id
        List<User> users = userRepository.findAllById(ids);

        log.debug("Найдено пользователей: {} из запрошенных {}", users.size(), ids.size());

        //Преобразование в DTO
        return users.stream()
                .map(userMapper::toFullDto)
                .toList();
    }
}