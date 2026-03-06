package com.itheima.xxydemo.service;

import com.itheima.xxydemo.model.Todo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TodoService {

    private final List<Todo> todos = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public TodoService() {
        addTodo("学习 GitHub Actions");
        addTodo("编写 Spring Boot 单元测试");
    }

    public List<Todo> findAll() {
        return Collections.unmodifiableList(todos);
    }

    public Todo addTodo(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        long id = idGenerator.incrementAndGet();
        Todo todo = new Todo(id, title, false);
        todos.add(todo);
        return todo;
    }
}

