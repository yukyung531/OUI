package com.emotionoui.oui.calendar.dto;


import com.emotionoui.oui.calendar.entity.Schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.sql.Date;
import java.time.LocalDateTime;

@Builder
public class ScheduleDto {

    @JsonProperty("member_id")
    private Integer memberId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("date")
    private LocalDateTime date;

    public ScheduleDto of(Schedule schedule){

        return  ScheduleDto.builder()
                .memberId(schedule.getMember().getId())
                .title(schedule.getTitle())
                .content(schedule.getContent())
                .date(schedule.getDate())
                .build();
    }
}
