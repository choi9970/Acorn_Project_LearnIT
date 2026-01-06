package com.learnit.learnit.admin.notice;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminNoticeRepository {

    List<AdminNoticeDto> selectNotices(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("category") String category,
            @Param("search") String search
    );

    int countNotices(
            @Param("category") String category,
            @Param("search") String search
    );

    AdminNoticeDto selectNoticeById(@Param("noticeId") int noticeId);

    // notice_id 직접 넣어서 insert (AUTO_INCREMENT 미사용)
    void insertNotice(AdminNoticeDto dto);

    void updateNotice(AdminNoticeDto dto);

    void deleteNotice(@Param("noticeId") int noticeId);

    void deleteNoticesByIds(@Param("ids") List<Integer> ids);

    // 전체삭제(필터 기반)
    void deleteAllByFilter(
            @Param("category") String category,
            @Param("search") String search
    );

    // 동시성 방지: MySQL Named Lock
    int getNoticeIdLock(@Param("lockName") String lockName);

    int releaseNoticeIdLock(@Param("lockName") String lockName);

    // 가장 작은 빈 번호(없으면 마지막+1, 비었으면 1)
    Integer selectSmallestMissingNoticeId();
}
