package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.UserService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final String pathInPublicApi = "/admin/users";
    private final String pathInFeignClient = "/feign/users";

    @PostMapping(pathInPublicApi)
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto addUser(@Valid @RequestBody UserRequest userRequest) {
        return userService.addUser(userRequest);
    }

    @GetMapping(pathInPublicApi)
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto> getAllUsers(@RequestParam(name = "ids", required = false) List<Long> ids,
                                     @RequestParam(name = "from", defaultValue = "0") Integer from,
                                     @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return userService.getUsers(ids, from, size);
    }

    @DeleteMapping(pathInPublicApi + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable(name = "id") Long id) {
        userService.deleteUser(id);
    }

    @GetMapping(pathInFeignClient + "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto getUser(@PathVariable(name = "id") Long id) {
        return userService.getUserById(id);
    }

    @GetMapping(pathInFeignClient + "/map")
    @ResponseStatus(HttpStatus.OK)
    public Map<Long, UserDto> getMapOfUsers(@RequestParam(name = "ids", required = false) List<Long> ids) {
        return userService.getAllUsers(ids);
    }
}
