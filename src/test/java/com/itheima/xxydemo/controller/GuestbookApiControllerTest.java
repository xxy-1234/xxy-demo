package com.itheima.xxydemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.xxydemo.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuestbookApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountService userAccountService;

    @BeforeEach
    void ensureAlice() {
        try {
            userAccountService.registerUser("alice", "alicepass12", "alice", "alice@test.example");
        } catch (IllegalArgumentException ignored) {
            // 已存在
        }
    }

    @Test
    void indexShouldDescribeProjectAndCountRoots() throws Exception {
        mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("GitHub Actions")))
                .andExpect(jsonPath("$.rootThreads", greaterThanOrEqualTo(0)));
    }

    @Test
    @WithUserDetails(value = "alice", userDetailsServiceBeanName = "databaseUserDetailsService")
    void postMessageShouldCreateRootThread() throws Exception {
        mockMvc.perform(
                        post("/api/messages")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"这是一条 API 留言\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author", is("alice")))
                .andExpect(jsonPath("$.content", is("这是一条 API 留言")))
                .andExpect(jsonPath("$.parentId", nullValue()));
    }

    @Test
    @WithUserDetails(value = "alice", userDetailsServiceBeanName = "databaseUserDetailsService")
    void postMessageShouldRejectBlankContent() throws Exception {
        mockMvc.perform(
                        post("/api/messages")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithUserDetails(value = "alice", userDetailsServiceBeanName = "databaseUserDetailsService")
    void postReplyShouldAttachToRoot() throws Exception {
        String created =
                mockMvc.perform(
                                post("/api/messages")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"content\":\"主贴用于回复测试\"}")
                        )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long rootId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(
                        post("/api/messages/" + rootId + "/replies")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"这是一条回复\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(rootId))
                .andExpect(jsonPath("$.content", is("这是一条回复")));
    }

    @Test
    void listMessagesShouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/messages").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    @WithUserDetails(value = "alice", userDetailsServiceBeanName = "databaseUserDetailsService")
    void toggleLikeShouldWorkOnRoot() throws Exception {
        String created =
                mockMvc.perform(
                                post("/api/messages")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"content\":\"待点赞主贴\"}")
                        )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long rootId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/messages/" + rootId + "/like").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked", is(true)))
                .andExpect(jsonPath("$.count", is(1)));

        mockMvc.perform(post("/api/messages/" + rootId + "/like").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked", is(false)))
                .andExpect(jsonPath("$.count", is(0)));
    }
}
