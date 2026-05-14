package com.itheima.xxydemo.service;

import com.itheima.xxydemo.model.Announcement;
import com.itheima.xxydemo.model.User;
import com.itheima.xxydemo.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public List<Announcement> listLatestForHome() {
        return announcementRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Announcement> listAllNewestFirst() {
        return announcementRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public Announcement create(User admin, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("公告标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }
        Announcement a = new Announcement();
        a.setTitle(title.trim());
        a.setContent(content.trim());
        a.setAuthorUsername(admin.getUsername());
        return announcementRepository.save(a);
    }

    @Transactional
    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }
}
