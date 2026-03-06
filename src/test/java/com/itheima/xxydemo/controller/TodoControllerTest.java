package com.itheima.xxydemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.xxydemo.model.Todo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void indexShouldReturnWelcomeMessageAndTodosCount() throws Exception {
        mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("GitHub Actions")))
                .andExpect(jsonPath("$.todosCount", greaterThanOrEqualTo(0)));
    }

    @Test
    void createTodoShouldCreateNewItem() throws Exception {
        Todo todo = new Todo();
        todo.setTitle("完成毕业论文");

        mockMvc.perform(
                        post("/api/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(todo))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("完成毕业论文")))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createTodoShouldRequireTitle() throws Exception {
        Todo todo = new Todo();
        todo.setTitle("");

        mockMvc.perform(
                        post("/api/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(todo))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("title is required")));
    }
}

