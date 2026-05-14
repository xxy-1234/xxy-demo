package com.itheima.xxydemo.repository;

import com.itheima.xxydemo.model.MessageLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MessageLikeRepository extends JpaRepository<MessageLike, Long> {

    boolean existsByMessage_IdAndUser_Id(Long messageId, Long userId);

    void deleteByMessage_IdAndUser_Id(Long messageId, Long userId);

    long countByMessage_Id(Long messageId);

    void deleteByMessage_Id(Long messageId);

    @Query("select ml.message.id, count(ml) from MessageLike ml where ml.message.id in :ids group by ml.message.id")
    List<Object[]> countLikesGrouped(@Param("ids") Collection<Long> ids);

    @Query("select ml.message.id from MessageLike ml where ml.message.id in :ids and ml.user.id = :uid")
    List<Long> findLikedMessageIds(@Param("ids") Collection<Long> ids, @Param("uid") Long userId);

    long count();
}
