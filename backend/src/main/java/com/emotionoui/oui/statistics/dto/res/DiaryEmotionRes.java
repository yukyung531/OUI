package com.emotionoui.oui.statistics.dto.res;

import com.emotionoui.oui.diary.dto.EmotionClass;
import com.emotionoui.oui.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.*;


@Builder
@Getter
@ToString
@AllArgsConstructor
public class DiaryEmotionRes {

    private String memberName;
    private Integer memberId;
    //내 월 감정
    private HashMap<String,Double> myMonthEmotion;

    // 내 개인 월
    private HashMap<String,Double> myPersonalEmotion;

    //친구 월 감정
    private List<DiaryMemberRes> members;

    public static DiaryEmotionRes of(Member member, Map<Integer, List<EmotionClass>> others, List<EmotionClass> personal, Map<Integer, String> othersName){

        Integer my = member.getMemberId();
        HashMap<String, Double> myEmotionSum = new HashMap<>();
        HashMap<String, Double> myPersonalEmotion = calculate(personal);
        List<DiaryMemberRes> memberResList = new ArrayList<>();

        others.forEach((memberId, emotionList) -> {
            // other에 key String 꺼내서 EmotionList총합을 계산
            HashMap<String, Double> emotionSum = calculate(emotionList);

            // 꺼낸 키가 mymemberId면 myEmotionSum으로
            if (Objects.equals(memberId, my)) {
                myEmotionSum.putAll(emotionSum);
            } else {
                // 다른 사용자의 정보는 memberRes에 추가
                memberResList.add(DiaryMemberRes.builder()
                        .memberId(memberId)
                        .emotion(emotionSum)
                        .nickname(othersName.get(memberId))
                        .build());
            }
        });

        return DiaryEmotionRes.builder()
                .memberName(member.getNickname())
                .memberId(my)
                .myMonthEmotion(myEmotionSum)
                .myPersonalEmotion(myPersonalEmotion)
                .members(memberResList)
                .build();

    }

    public static HashMap<String, Double> calculate(List<EmotionClass> emotions) {
        HashMap<String, Double> emotionSums = new HashMap<>();

        // 초기값 설정
        emotionSums.put("happy", 0.0);
        emotionSums.put("comfortable", 0.0);
        emotionSums.put("embarrassed", 0.0);
        emotionSums.put("angry", 0.0);
        emotionSums.put("doubtful", 0.0);
        emotionSums.put("sad", 0.0);

        for (EmotionClass emotion : emotions) {
            if(emotion==null){
                continue;
            }
            emotionSums.put("happy", emotionSums.get("happy") + emotion.getHappy());
            emotionSums.put("comfortable", emotionSums.get("comfortable") + emotion.getComfortable());
            emotionSums.put("embarrassed", emotionSums.get("embarrassed") + emotion.getEmbarrassed());
            emotionSums.put("angry", emotionSums.get("angry") + emotion.getAngry());
            emotionSums.put("doubtful", emotionSums.get("doubtful") + emotion.getDoubtful());
            emotionSums.put("sad", emotionSums.get("sad") + emotion.getSad());
        }
        // 모두 양수로 만들기
        double min = Double.MAX_VALUE;
        for(Double value: emotionSums.values()){
            if(value<min)
                min = value;
        }
        for(String key: emotionSums.keySet()){
            emotionSums.put(key, emotionSums.get(key)-min+1);
        }

        boolean flag = true;

        for (double value : emotionSums.values()) {
            if (value != 1) {
                flag = false;
                break;
            }
        }

        // 총합 구해서
        double totalSum = 0;
        for (Double value: emotionSums.values()) {
            totalSum += value;
        }
        //백분율로
        for (String key : emotionSums.keySet()) {
            double percentage = (emotionSums.get(key) / totalSum) * 100;
            double roundedPercentage = Math.round(percentage);
            emotionSums.put(key, roundedPercentage);
        }
        if (flag) {
            // 모든 값이 0인 경우, 모든 키에 대해 값을 0으로 설정합니다.
            for (String key : emotionSums.keySet()) {
                emotionSums.put(key, 0.0);
            }
        }

        return emotionSums;
    }

}
