package com.emotionoui.oui.member.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMemberReq {
    private String memberNickname;
    private String preference;
    private MultipartFile ImgUrl;

    public UpdateMemberReq(String memberNickname, String preference, MultipartFile imgUrl) {
        this.preference = preference;
        this.memberNickname = memberNickname;
        ImgUrl = imgUrl;
    }

    public UpdateMemberReq(String memberNickname) {
        this.memberNickname = memberNickname;
    }
}
