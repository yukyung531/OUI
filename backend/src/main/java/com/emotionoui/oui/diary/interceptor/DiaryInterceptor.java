package com.emotionoui.oui.diary.interceptor;

import com.emotionoui.oui.auth.jwt.JwtUtil;
import com.emotionoui.oui.member.entity.Member;
import com.emotionoui.oui.member.repository.MemberRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
// WebSocket 요청을 intercept하여 JWT Token을 확인하고 해당 사용자의 인증 정보를 가져오는 역할을 함
public class DiaryInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    /**
     * 메세지를 보내기 전에 실행되는 interceptor 메소드
     *
     * @param message 전송될 메시지. 이 메시지의 header에는 JWT Token이 포함되어 있어야 함
     * @param channel 메시지가 전송될 채널
     * @return 수정된 메시지를 반환 (사용자 인증 정보가 추가된 페이지)
     */
    @Transactional(readOnly = true)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        StompCommand command = accessor.getCommand();
        log.info(command.toString());

        if (StompCommand.SUBSCRIBE.equals(command)) {
            log.info("DiaryInterceptor");
            String[] destination = accessor.getDestination().split("/");
            log.info(destination[destination.length - 1].replace("diaryId", ""));
            setValue(accessor, "dailyId", Long.parseLong(destination[destination.length - 1].replace("diary", "")));
        }

        if (StompCommand.CONNECT.equals(command)) {
            // 메시지 헤더에서 Authorization 정보(JWT Token이 포함됨)를 가져옴
            String authToken = accessor.getFirstNativeHeader("Authorization");

            if (authToken != null && authToken.startsWith("Bearer ")) {
                log.info("토큰 null 아니면 여기로 옴");
                String jwtToken = authToken.substring(7);
                try {
                    log.info("try 시작부분");
                    // 0 : 유효 , 1 : 만료 , 2 : 이상한 토큰
                    if (jwtUtil.isExpired(jwtToken) == 1) {
                        log.info("만료되면 찍히는 로그");
                        throw new JwtException("토큰이 만료되었습니다.");
                    }
                    log.info("만료 안되면 찍히는 로그");
                    String email = jwtUtil.getEmail(jwtToken);
                    log.info("email: " + email);
                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(IllegalArgumentException::new);

                    log.info("야 너 어딨어 " + member.getMemberId());

                    // 스프링 시큐리티 인증 토큰 생성
                    Authentication authentication = new UsernamePasswordAuthenticationToken(member, null, member.getAuthorities());
                    // 사용자 정보를 메시지 헤더에 저장
                    accessor.setUser(authentication);
                } catch (JwtException e) {
                    // Exception 써야해요 Exception 좀 쓰자...
                }
            }
        }
        return message;
    }

    private void setValue(StompHeaderAccessor accessor, String key, Object value) {
        Map<String, Object> sessionAttributes = getSessionAttributes(accessor);
        sessionAttributes.put(key, value);
    }

    private Map<String, Object> getSessionAttributes(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        return sessionAttributes;
    }
}
