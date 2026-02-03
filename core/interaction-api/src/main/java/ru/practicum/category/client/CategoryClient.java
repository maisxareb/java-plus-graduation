package ru.practicum.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.category.dto.CategoryDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "category-service", path = CategoryClient.FEIGN_PATH)
public interface CategoryClient {
    String FEIGN_PATH = "/feign/categories";
    String ID_PATH = "/{catId}";

    @GetMapping(ID_PATH)
    CategoryDto getById(@PathVariable(name = "catId") Long categoryId);

    @GetMapping
    Map<Long, CategoryDto> getMap(@RequestParam(name = "ids") List<Long> ids);
}
