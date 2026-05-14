package com.itheima.xxydemo.repository;

import com.itheima.xxydemo.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findTop5ByOrderByCreatedAtDesc();
}
