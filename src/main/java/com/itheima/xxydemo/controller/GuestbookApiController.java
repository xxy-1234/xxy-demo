package com.itheima.xxydemo.controller;

import com.itheima.xxydemo.dto.ContentPayload;
import com.itheima.xxydemo.dto.GuestbookMessageDto;
import com.itheima.xxydemo.model.GuestbookThread;
import com.itheima.xxydemo.model.MessageCategory;
import com.itheima.xxydemo.security.AccountPrincipal;
import com.itheima.xxydemo.service.GuestbookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class GuestbookApiController {

    private final GuestbookService guestbookService;

    public GuestbookApiController(GuestbookService guestbookService) {
        this.guestbookService = guestbookService;
    }

    @GetMapping("/")
    public ResponseEntity<ProjectInfo> index() {
        return ResponseEntity.ok(
                new ProjectInfo(
                        "课题：基于 GitHub Actions 的自动化测试与部署流程（GitHub Actions + 阿里云 ECS + MySQL 留言板示例）。",
                        guestbookService.countRootMessages()
                )
        );
    }

    @GetMapping("/messages")
    public ResponseEntity<MessagesPage> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "8") @Min(1) @Max(30) int size,
            @AuthenticationPrincipal AccountPrincipal principal) {
        var p =
                guestbookService.listThreads(
                        PageRequest.of(page, size),
                        principal != null ? principal.getUser() : null,
                        null,
                        null);
        return ResponseEntity.ok(
                new MessagesPage(
                        p.getContent(),
                        p.getTotalPages(),
                        p.getTotalElements(),
                        p.getNumber(),
                        p.getSize()
                )
        );
    }

    @PostMapping("/messages")
    public ResponseEntity<GuestbookMessageDto> create(
            @RequestBody @Valid ContentPayload body,
            @AuthenticationPrincipal AccountPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MessageCategory c = GuestbookService.parseCategory(body.getCategory());
        var saved = guestbookService.postRoot(principal.getUser(), body.getTitle(), body.getContent(), c);
        return ResponseEntity.status(HttpStatus.CREATED).body(GuestbookMessageDto.from(saved));
    }

    @PostMapping("/messages/{parentId}/replies")
    public ResponseEntity<GuestbookMessageDto> reply(
            @PathVariable Long parentId,
            @RequestBody @Valid ContentPayload body,
            @AuthenticationPrincipal AccountPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var saved = guestbookService.postReply(principal.getUser(), parentId, body.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(GuestbookMessageDto.from(saved));
    }

    @PostMapping("/messages/{id}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean liked = guestbookService.toggleLike(principal.getUser(), id);
        long count = guestbookService.getLikeCount(id);
        return ResponseEntity.ok(new LikeResponse(liked, count));
    }

    public record ProjectInfo(String message, long rootThreads) {
    }

    public record MessagesPage(
            List<GuestbookThread> content,
            int totalPages,
            long totalElements,
            int page,
            int size
    ) {
    }

    public record LikeResponse(
            boolean liked,
            long count
    ) {
    }
}
