package com.emotionoui.oui.member.repository;

import com.emotionoui.oui.member.entity.Member;
import com.emotionoui.oui.schedule.entity.Schedule;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByEmail(String email);

//    @Query("SELECT m. FROM Member m " +
//            "JOIN FETCH m.memberAlarmList a " +
////            "JOIN FETCH m.preferenceList p " +
////            "JOIN FETCH m.scheduleList s " +
//            "JOIN FETCH m.emotionList e " +
////            "JOIN FETCH m.memberDiaryList d " +
//            "WHERE m.email = :email")
//    Optional<Member> findByEmailFetchJoin(@Param("email") String email);

    Optional<Member> findByMemberIdAndIsDeleted(Integer memberId, Integer isDeleted);
}
