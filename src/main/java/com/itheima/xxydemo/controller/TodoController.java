package com.itheima.xxydemo.controller;

import com.itheima.xxydemo.model.Todo;
import com.itheima.xxydemo.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/")
    public ResponseEntity<IndexResponse> index() {
        List<Todo> todos = todoService.findAll();
        IndexResponse response = new IndexResponse(
                "欢迎使用基于 GitHub Actions 的 Spring Boot CI/CD 演示应用1。",
                todos.size()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/todos")
    public List<Todo> getTodos() {
        return todoService.findAll();
    }

    @PostMapping("/todos")
    public ResponseEntity<?> createTodo(@RequestBody Todo request) {
        try {
            Todo created = todoService.addTodo(request.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    public record IndexResponse(String message, int todosCount) {
    }

    public record ErrorResponse(String error) {
    }
}

