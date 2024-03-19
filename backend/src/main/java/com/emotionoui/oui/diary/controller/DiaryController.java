package com.emotionoui.oui.diary.controller;

import com.emotionoui.oui.diary.dto.EmotionClass;
import com.emotionoui.oui.diary.dto.req.CreateDailyDiaryReq;
import com.emotionoui.oui.diary.dto.req.UpdateDailyDiaryReq;
import com.emotionoui.oui.diary.dto.res.SearchDailyDiaryRes;
import com.emotionoui.oui.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // 일기 게시글 작성하기
    @PostMapping
    public ResponseEntity<?> createDailyDiary(@RequestBody CreateDailyDiaryReq req) throws IOException, ExecutionException, InterruptedException {
        // 작성자가 필요함

//        log.info("다이어리왔음? " + String.valueOf(req.getDiaryId()));
//        log.info("다이어리왔음? " + req.getDailyContent());
//        return new ResponseEntity<String>("1", HttpStatus.OK);

        return new ResponseEntity<String>(diaryService.createDailyDiary(req), HttpStatus.OK);
    }

    // 일기 게시글 수정하기
    @PutMapping("/{dailyId}")
    public ResponseEntity<?> updateDailyDiary(@RequestBody UpdateDailyDiaryReq req){
        return new ResponseEntity<String>(diaryService.updateDailyDiary(req), HttpStatus.OK);
    }

    // 일기 게시글 삭제하기
    @PutMapping("/delete/{dailyId}")
    public ResponseEntity<?> deleteDailyDiary(){
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 일기 게시글 조회하기
    @GetMapping("/{dailyId}")
    public ResponseEntity<?> searchDailyDiary(@PathVariable String dailyId){
        return new ResponseEntity<SearchDailyDiaryRes>(diaryService.searchDailyDiary(dailyId), HttpStatus.OK);
    }

    @GetMapping("/emotion/{dailyId}")
    public ResponseEntity<?> searchEmotion(@PathVariable String dailyId){
        return new ResponseEntity<EmotionClass>(diaryService.searchEmotion(dailyId), HttpStatus.OK);
    }
}