# ■ OUI

#### 💻 주요 코드

##### 1. login 사용자 인증 과정

📌 <u>**Member.java**</u> 

```java
package com.emotionoui.oui.member.entity;

...

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name="MEMBER")
public class Member implements UserDetails {

    @Id
    @Column(name="member_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer memberId;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="nickname")
    private String nickname;

    @Column(name="img")
    private String img;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name="regdate", nullable = false)
    private LocalDateTime regdate;

    @Column(name="is_deleted", nullable = false)
    private int isDeleted = 0;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isDeleted == 0;
    }

    ...

    public Member(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    @Builder
    public Member(int memberId, String email, String nickname, String img, LocalDateTime regdate){
        this.memberId= memberId;
        this.email = email;
        this.nickname = nickname;
        this.img = img;
        this.regdate= regdate;
    }
}
```

- Spring Security에서 UserDetails객체를 사용하므로 UserDetails를 구현하여 MemberEntity를 구현하였다.

<br>

📌 <u>**JwtFilter.java**</u> 

```java
package com.emotionoui.oui.auth.jwt;

...

public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public JwtFilter(JwtTokenProvider jwtTokenProvider, JwtUtil jwtUtil, MemberRepository memberRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    /**
     * 헤더에 담겨 온 accessToken으로 사용자 정보 생성
     * @param request
     * @param response
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        String token = jwtTokenProvider.resolveToken(request);
        int check = jwtTokenProvider.validateToken(token);

        // 토큰이 존재하고, 유효하다면
        if (token != null && check==0) {
            
            // 토큰을 통해 인증 정보 생성
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            
            //토큰에서 email 획득
            String email = jwtUtil.getEmail(token);

            //member를 생성하여 값 set
            Member member;
            try{
                member = memberRepository.findByEmail(email).orElseThrow(Exception::new);

                // 스프링 시큐리티 인증 토큰 생성
                Authentication authToken = new UsernamePasswordAuthenticationToken
                    (member, null, member.getAuthorities());
                
                // SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }catch (Exception e){
                throw new MemberNotFoundException(e);
            }
        }
        else if(token != null && check==1){ // 만료된 토큰이라면 새로운 토큰 발급
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드 설정
            response.getWriter().write("만료된 accessToken입니다. 토큰을 재요청하세요.");
            return;
        }
        else if(token == null || check==2){ // 토큰이 없거나 이상한 토큰이라면 400 badRequest
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 상태 코드 설정
            response.getWriter().write("accessToken이 없거나 이상합니다. 다시 로그인하세요.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

```

- header에 담겨 온 accessToken을 통한 사용자 인증을 위한 `JwtFilter`를 구현하였다.
  - 유효한 토큰이 확인되면, 토큰으로부터 사용자의 이메일을 추출하고, 이 이메일을 통해 데이터베이스에서 사용자 정보를 조회한다. 이후, 조회된 사용자 정보를 바탕으로 스프링 시큐리티의 `Authentication` 객체를 생성하고, `SecurityContext`에 저장하여 사용자가 인증된 것으로 처리한다.

<br>

📌 <u>**JwtProvider.java**</u> 

```java
package com.emotionoui.oui.auth.jwt;

...

@Component
public class JwtTokenProvider {

    private final JwtUtil jwtUtil;

    private Long accessTokenExpireTime = 1000 * 60 * 60L; // 1시간

    private Long refreshTokenExpireTime = 1000 * 60 * 60 * 24 * 3L; // 3일
    @Autowired
    public JwtTokenProvider(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // 토큰에서 회원 구별 정보 추출 (여기서는 이메일)
    public String getEmailFromToken(String token) {
        return jwtUtil.getEmail(token);
    }

    // 토큰의 유효성 + 만료일자 확인
    public int validateToken(String token) {
        return jwtUtil.isExpired(token);
    }

    // 액세스 토큰 생성
    public String createAccessToken(String email) {
        return jwtUtil.createJwt(email, accessTokenExpireTime);
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(String email) {
        return jwtUtil.createJwt(email, refreshTokenExpireTime);
    }

    // 리프레시 토큰을 쿠키에 담는 메서드
    public void createRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        // 쿠키 만료 시간 설정 ( 3일 )
        refreshTokenCookie.setMaxAge(3 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
    }

    // 요청에서 토큰을 해석하는 메소드
    public String resolveToken(HttpServletRequest request) {
        // 헤더에서 "Authorization" 값을 가져와서 토큰을 해석하는 메소드
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 다음의 토큰 값을 반환
        }
        return null;
    }

    // 토큰을 기반으로 사용자 인증 정보를 생성하는 메소드
    public Authentication getAuthentication(String token) {
        // 토큰에서 사용자 정보(이메일)를 추출
        String email = getEmailFromToken(token);
        // UserDetails 객체 생성
        UserDetails userDetails = new User(email, "", new ArrayList<>());
        // 최종적으로 Authentication 객체 반환
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

}

```

-  JWT 토큰을 생성하고 검증하는 작업을 담당하는 `JwtTokenProvider` 을 구현하였다. 
  1. **토큰 생성:** 사용자의 이메일을 기반으로 액세스 토큰과 리프레시 토큰을 생성. 
     - 액세스 토큰은 짧은 기간(1시간) 동안 유효. 
     - 리프레시 토큰은 더 긴 기간(3일) 동안 유효.
  2. **토큰 검증:** 제공된 토큰이 유효한지, 만료되었는지를 확인.
  3. **토큰에서 사용자 정보 추출:** 토큰에서 사용자의 이메일 같은 구별 정보를 추출.
  4. **리프레시 토큰 쿠키 생성:** 
     - 리프레시 토큰을 HTTP 쿠키에 저장하여, 클라이언트가 이를 안전하게 보관할 수 있도록 하였다.
     - HttpOnly와 Secure 속성을 사용하여 XSS 공격과 데이터 유출 위험을 줄였다.
  5. **요청에서 토큰 추출:** 헤더에서 "Authorization" 값을 읽어, "Bearer"로 시작하는 토큰을 추출.
  6. **사용자 인증 정보 생성:** 
     - 추출된 토큰에서 사용자 정보를 바탕으로 `UserDetails` 객체를 생성하고, 이를 사용하여 `Authentication` 객체를 생성하였다.

<br>

📌 <u>**JwtUtil.java**</u> 

```java
package com.emotionoui.oui.auth.jwt;

...
    
@Component
public class JwtUtil {
    private Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] byteSecretKey = Decoders.BASE64.decode(secret);
        key = Keys.hmacShaKeyFor(byteSecretKey); // 객체 key 생성
    }

    // Member email 검증
    public String getEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("email", String.class);
    }

    // 토큰 만료 일자 검증
    public int isExpired(String token) {
        int check = 0; // 0 : 유효 , 1 : 만료 , 2 : 이상한 토큰
        try {
            if (!Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration().before(new Date(System.currentTimeMillis()))) {
                check = 0;
            };
        } catch (ExpiredJwtException e) {
            check = 1;
        } catch (Exception e) {
            check = 2;
        }
        return check;
    }

    // 토큰 생성
    public String createJwt(String email, Long expiredMs) {
        Claims claims = Jwts.claims();
        claims.put("email", email);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256) //암호화
                .compact();
    }
}

```

- JWT 토큰을 생성하고, 검증하는 핵심 기능을 담당하는 `JwtUtil` 을 구현하였다.
  1. **생성자에서의 키 설정:** 클래스가 생성될 때, `@Value` 어노테이션을 사용하여 `application.properties` 파일 등에서 설정된 JWT 비밀키(`jwt.secret`)를 가져와서, 이를 기반으로 한 HMAC SHA 키를 생성. 이 키는 토큰의 서명에 사용된다.
  2. **이메일 추출:** 제공된 토큰에서 사용자의 이메일을 추출. 이는 토큰의 클레임에서 "email" 키에 해당하는 값을 가져옴으로써 이루어진다.
  3. **토큰 만료 검증:** 토큰이 만료되었는지 여부를 검증
  4. **토큰 생성:** 사용자의 이메일과 만료 시간(밀리초 단위)을 입력 받아 JWT 토큰을 생성. 토큰에는 발행 시간(`issuedAt`), 만료 시간(`setExpiration`), 그리고 클레임으로 사용자의 이메일이 포함된다. 이후 HS256 알고리즘을 사용하여 해당 키로 서명하여 토큰을 완성한다.

<br>

##### 2. Querydsl

📌 <u>**QuerydslRepositoryImpl.java**</u> 

```java
package com.emotionoui.oui.querydsl;

...

@Repository
public class QuerydslRepositoryImpl implements QuerydslRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    @Autowired
    public QuerydslRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    // 다이어리 리스트 조회
    @Override
    public List<SearchDiaryListRes> findDiariesByMemberId(int memberId) {
        QMemberDiary memberDiary = QMemberDiary.memberDiary;
        QDiary diary = QDiary.diary; // Q-타입 인스턴스 생성

        return queryFactory
                .select(Projections.constructor(SearchDiaryListRes.class,
                        diary.id.as("diaryId"), // Projections.constructor를 사용할 때, as(" ") 값은 SearchDiaryListRes 클래스 생성자 파라미터 이름과 일치해야 함
                        diary.name.as("diaryName"),
                        diary.templateId.as("templateId"),
                        memberDiary.member.memberId.as("memberId"),
                        diary.type.as("diaryType"),
                        diary.createdAt.as("createdAt"),
                        memberDiary.orders.as("orders")
                ))
                .from(memberDiary) // memberDiary 테이블을 기준으로 조회
                .join(memberDiary.diary, diary) // memberDiary와 diary를 조인(memberDiary.diary는 memberDiary 테이블과 diary 테이블을 연결하는 외래키)
                .where(memberDiary.member.memberId.eq(memberId) // memberId가 주어진 memberId와 일치하고,
                        .and(memberDiary.isDeleted.eq(0))
                        .and(diary.isDeleted.eq(0))) // 삭제처리 되지 않은 행만 조회
                .orderBy(memberDiary.orders.asc()) // orders 컬럼을 기준으로 오름차순 정렬
                .fetch();
    }

    // 회원탈퇴 시 일정 삭제 처리
    @Override
    public void deleteSchedleByMemberId(int memberId) {
        QSchedule schedule = QSchedule.schedule;

        queryFactory
                .update(schedule)
                .set(schedule.isDeleted,1)
                .where(schedule.member.memberId.eq(memberId).and(schedule.isDeleted.eq(0)))
                .execute();
    }

    // 회원탈퇴 시 멤버다이어리 삭제 처리
    @Override
    public void deleteMemberDiaryByMemberId(int memberId) {
        QMemberDiary memberDiary = QMemberDiary.memberDiary;

        queryFactory
                .update(memberDiary)
                .set(memberDiary.isDeleted,1)
                .where(memberDiary.member.memberId.eq(memberId).and(memberDiary.isDeleted.eq(0)))
                .execute();
    }

    // 회원탈퇴 시 알람 삭제 처리
    @Override
    public void deleteAlarmByMemberId(int memberId) {
        QMemberAlarm memberAlarm = QMemberAlarm.memberAlarm;

        queryFactory
                .update(memberAlarm)
                .set(memberAlarm.isDeleted,1)
                .where(memberAlarm.member.memberId.eq(memberId).and(memberAlarm.isDeleted.eq(0)))
                .execute();
    }

    // 회원탈퇴 시 취향 삭제 처리
    @Override
    public void deletePreferenceByMemberId(int memberId) {
        QPreference preference = QPreference.preference;

        queryFactory
                .update(preference)
                .set(preference.isDeleted,1)
                .where(preference.member.memberId.eq(memberId))
                .execute();
    }
    
    ...
        
```

- JPA로 처리하기 어려운 다소 복잡한 쿼리문을 작성하기 위해 Querydsl을 사용하였다.
  - 타입 안전성과 코드 가독성을 위해 JPQL을 사용하지 않았다.


<br>

---

## 🙋🏻‍♀️ 마무리

<br>

### 🔹어려웠던 점(원인, 해결, 느낀점)

#### 1. (로그인 기능을 구현하면서) 사용자 인증 과정에 어려움을 겪었다.

- **원인:**
- 내가 Spring Security에 대한 학습이 부족했기 때문에 발생
- **해결:**
  - MemberEntity를 UserDetails의 구현체로 만들어서 해결하였다.
    - `UserDetails` : Spring Security가 사용자의 인증을 처리하는 데 필요한 사용자 정보를 제공하는 사용자의 정보를 담고 있는 인터페이스

- **느낀점**:

  - 이전 프로젝트에서 로그인을 담당한 팀원이 어려움을 겪는 것을 보고, 나도 로그인 기능에 도전해보고 싶은 마음이 생겨서 이번 프로젝트에서 도전하게 되었다.
  시간이 부족한 탓에 완벽하게 학습하지 못하고 기능 구현을 시작하였다. 그 때문에 시간이 오히려 더 걸린 것 같다. 이번 프로젝트는 끝났지만 이번 기회에 Spring Security에 대해 완벽하게 학습을 해야겠다는 생각을 했다.

### 🔹아쉬운 점

- 기획 기간이 길어져서 내가 맡은 부분에 대한 공부를 완벽하게 하지 못하고 구현한 것이 아쉬웠다.
  로그인 기능을 구현하는 데 있어서, Security 과정에 대해 이해하는 것은 매우 중요한데 마음이 급해서 이를 완전히 이해하지 못하고 개발을 시작하였다. 그러다보니 구현 시간도 오래 걸렸고, 완성한 로그인 기능에 대해서 나조차도 완벽히 이해하지 못해 아쉬웠다. Security 과정에 대해 다시 공부하는 시간을 가지면서 해당 기능을 완벽하게 내 것으로 만들려고 한다.
- 예외처리가 완벽하게 되지 않은 부분들이 있다. 마감 시간이 촉박해서 이또한 완벽하게 처리하지 못하였는데, 이후 계속 리팩토링하면서 완성도를 높일 계획이다.
- 팀원들과 매일 DailyScrum을 하면서 진행상황을 공유하였지만, 중간중간 소통해야 할 것들이 잘 소통되지 않아 아쉽다. 다들 마음이 급하다보니 '나중에 얘기해야지.' 생각하며, 소통하지 않고 깜빡하고 넘어가는 부분들이 많았다. 그로 인해 git이 꼬인다든지, 여러 사람이 같은 작업을 한다든지 하는 문제들이 발생하였다. 이 경험을 통해 소통의 중요성을 다시 한번 느꼈고, 이후 프로젝트를 진행할 때에는 그때그때 소통가능한 채팅방에라도 전달사항을 남겨놓는 것을 규칙으로 정하면 좋을 것 같다.
- 처음으로 Querydsl을 사용해 보았는데, 복잡한 동적 쿼리를 사용해보지 못한 것이 아쉽다. Querydsl에 대해 더 공부하여 복잡한 동적 쿼리도 익숙해질 수 있도록 해야겠다.

<br>

### 🔹느낀 점

- 이번 프로젝트를 하면서 도전해보고 싶었던 로그인 기능을 구현할 수 있어서 좋았다.

  아직 완벽하게 이해했다고 말할 수는 없지만 완벽하게 이해하기 위해서 지속적으로 공부할 계획이다. 

  또한 이번 프로젝트에서 Querydsl을 처음 사용해보았는데, 다음 프로젝트를 할 때에는 더 학습해서 복잡한 동적쿼리도 사용해보고 싶다.

- 이번 프로젝트에서도 지난 번에 이어 디자인 부분에 많이 참여하였다. 디자인이 쉽지은 않지만 생각보다 더 재미있는 것 같다.

- 팀원들이 긍적적이어서 분위기가 좋았다. 누군가 의견을 내면 긍정적으로 받아들여줘서 의견을 자유롭게 말할 수 있는 분위기였다. 좋은 팀원을 만나는 것이 프로젝트 진행에 있어 중요한 부분이라는 것을 다시 느끼게 되었다.

<br>

### 🔹배운 점

- jwt를 이용한 로그인 과정에 대해 잘 이해하게 되었다.

- 자주 사용되는 annotation의 의미들을 어렴풋이 알고 있었는데, 이번 프로젝트를 진행하면서 개념을 확실히 이해하기 위해 노력했습니다.
  - **@Entity**
    - Spring Data JPA에서 사용되며, 클래스가 JPA 엔티티임을 나타낸다. 이 주석이 적용된 클래스는 데이터베이스 테이블에 매핑된다.
  - **@Service**
    - 서비스 계층의 구성 요소임을 나타내며, 비즈니스 로직을 처리하는 데 사용된다.
  - **@NoArgsConstructor**
    - Lombok 라이브러리에서 제공하는 것으로, 파라미터가 없는 기본 생성자를 자동으로 생성한다.
  - **@ToString**
    - Lombok에서 제공하는 것으로, 클래스의 `toString()` 메서드를 자동으로 생성한다. 클래스의 필드를 기반으로 문자열 표현을 생성한다.
  - **@AllArgsConstructor**
    - Lombok에서 제공하는 것으로, 모든 필드 값을 파라미터로 받는 생성자를 자동으로 생성한다.
  - **@RequiredArgsConstructor**
    - Lombok에서 제공하는 것으로, final 필드나 `@NonNull`이 붙은 필드에 대한 생성자를 자동으로 생성한다.
  - **@Controller**
    - 스프링 MVC에서 컨트롤러를 나타낸다.
  - **@RestController**
    - `@Controller`에 `@ResponseBody`를 추가한 것으로 볼 수 있으며, RESTful 웹 서비스의 컨트롤러를 나타낸다.
  - **@Component**
    - Spring에서 관리하는 컴포넌트(빈)임을 나타낸다. `@Repository`, `@Service`, `@Controller` 등은 모두 `@Component`의 특수한 경우이다.
  - **@Value**
    - 프로퍼티 파일 등의 외부 설정을 필드에 주입할 때 사용한다. 예를 들어, `@Value("${some.value}")` 형태로 사용된다.
