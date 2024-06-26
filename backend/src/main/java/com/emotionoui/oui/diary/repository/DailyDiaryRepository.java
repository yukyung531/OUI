package com.emotionoui.oui.diary.repository;

import com.emotionoui.oui.diary.entity.DailyDiary;
import com.emotionoui.oui.statistics.dto.WeeklyMongoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;


@Repository
public interface DailyDiaryRepository extends JpaRepository<DailyDiary, Integer> {

    @Query(value = "SELECT new com.emotionoui.oui.statistics.dto.WeeklyMongoDto(d.mongoId,d.dailyDate) " +
            "FROM DailyDiary d " +
            "WHERE d.diary.id = :diaryId AND d.dailyDate BETWEEN :start AND :end AND d.isDeleted = 0")
    List<WeeklyMongoDto> getMongoIdByDiaryId(Integer diaryId, Date start, Date end);

    @Query("SELECT d.id FROM DailyDiary d WHERE d.mongoId = :mongoId")
    Integer findDailyDiaryIdByMongoId(String mongoId);

    @Query("SELECT d.id FROM DailyDiary d JOIN Emotion e ON d.id = e.dailyDiary.id WHERE d.diary.id = :diaryId AND e.member.memberId = :memberId AND d.dailyDate = :date AND d.isDeleted = 0")
    Integer findTodayDailyId(@Param("date") Date date, @Param("memberId") Integer memberId, @Param("diaryId") Integer diaryId);

}
