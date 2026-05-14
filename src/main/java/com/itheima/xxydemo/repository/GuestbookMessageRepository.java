package com.itheima.xxydemo.repository;

import com.itheima.xxydemo.model.GuestbookMessage;
import com.itheima.xxydemo.model.MessageCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GuestbookMessageRepository extends JpaRepository<GuestbookMessage, Long> {

    Page<GuestbookMessage> findByParentIdIsNullOrderByCreatedAtDesc(Pageable pageable);

    @Query(
            countQuery = """
                    select count(m) from GuestbookMessage m
                    where m.parentId is null
                    and (:includeBlocked = true or m.blocked = false)
                    and (:qEmpty = true or lower(m.title) like lower(concat('%', :q, '%')) or lower(m.content) like lower(concat('%', :q, '%')))
                    and (:cat is null or m.category = :cat)
                    """,
            value = """
                    select distinct m from GuestbookMessage m
                    left join fetch m.authorUser
                    where m.parentId is null
                    and (:includeBlocked = true or m.blocked = false)
                    and (:qEmpty = true or lower(m.title) like lower(concat('%', :q, '%')) or lower(m.content) like lower(concat('%', :q, '%')))
                    and (:cat is null or m.category = :cat)
                    """)
    Page<GuestbookMessage> findRootsForPublicOrAdmin(
            @Param("q") String q,
            @Param("qEmpty") boolean qEmpty,
            @Param("cat") MessageCategory cat,
            @Param("includeBlocked") boolean includeBlocked,
            Pageable pageable);

    @Query(
            """
                    select m from GuestbookMessage m
                    left join fetch m.authorUser
                    where m.parentId = :parentId
                    order by m.createdAt asc
                    """)
    List<GuestbookMessage> findByParentIdOrderByCreatedAtAsc(@Param("parentId") Long parentId);

    @Query("select m from GuestbookMessage m left join fetch m.authorUser where m.id = :id")
    Optional<GuestbookMessage> findDetailById(@Param("id") Long id);

    boolean existsByIdAndParentIdIsNull(Long id);

    long countByParentIdIsNull();

    long countByParentIdIsNotNull();

    long countByCreatedAtAfter(Instant after);

    Page<GuestbookMessage> findByAuthorContainingIgnoreCaseOrderByCreatedAtDesc(String authorPart, Pageable pageable);

    List<GuestbookMessage> findByAuthorUser_IdAndParentIdIsNullOrderByCreatedAtDesc(Long userId);

    List<GuestbookMessage> findByAuthorUser_IdAndParentIdIsNotNullOrderByCreatedAtDesc(Long userId);

    void deleteByParentId(Long parentId);
}
