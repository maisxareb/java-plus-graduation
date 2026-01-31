package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.StatisticsGetResponseDto;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.mappers.StatMapper;
import ru.practicum.model.Statistics;
import ru.practicum.repository.StatisticsRepository;
import ru.practicum.repository.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final StatisticsRepository statisticsRepository;
    private final StatMapper statMapper; // Добавляем внедрение маппера

    @Transactional
    @Override
    public StatisticsPostResponseDto hit(StatisticsPostResponseDto statisticsPostResponseDto) {
        // Используем внедренный маппер, а не статический вызов
        Statistics statistics = statMapper.fromDto(statisticsPostResponseDto);
        Statistics savedStatistics = statisticsRepository.save(statistics);
        return statMapper.toDto(savedStatistics);
    }

    @Transactional(readOnly = true) // Для read-операций лучше использовать readOnly = true
    @Override
    public List<StatisticsGetResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new RuntimeException("Время начала не может быть позднее времени конца выборки");
        }
        List<ViewStats> rows = switch ((unique ? 2 : 0) + ((uris != null && !uris.isEmpty()) ? 1 : 0)) {
            case 0 -> statisticsRepository.findStats(start, end);
            case 1 -> statisticsRepository.findStatsByUris(start, end, uris);
            case 2 -> statisticsRepository.findUniqueStats(start, end);
            case 3 -> statisticsRepository.findUniqueStatsByUris(start, end, uris);
            default -> throw new RuntimeException("Некорректные параметры запроса");
        };

        return rows.stream()
                .map(v -> new StatisticsGetResponseDto(v.getApp(), v.getUri(), v.getHits()))
                .toList();
    }
}
