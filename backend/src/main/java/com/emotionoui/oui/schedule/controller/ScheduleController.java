package com.emotionoui.oui.schedule.controller;


import com.emotionoui.oui.member.entity.Member;
import com.emotionoui.oui.schedule.dto.req.ScheduleReq;
import com.emotionoui.oui.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 일정 등록
    @PostMapping("/my")
    public ResponseEntity<?> saveSchedules(@RequestBody ScheduleReq scheduleReq,
                                           @AuthenticationPrincipal Member member) {

        int memberId = scheduleService.saveSchedules(scheduleReq, member);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 일정 수정
    @PutMapping("/my/{scheduleId}")
    public ResponseEntity<?> updateSchedule(@RequestBody ScheduleReq scheduleReq,
                                            @PathVariable(value= "scheduleId") Integer scheduleId){
        scheduleService.updateSchedules(scheduleId, scheduleReq);
        return ResponseEntity.noContent().build();
    }

    //일정 삭제 - isDelete 변경
    @PutMapping("/my/{scheduleId}/delete")
    public ResponseEntity<?> deleteSchedule(@PathVariable(value= "scheduleId") Integer scheduleId){
        scheduleService.deleteSchedules(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
