package com.StreetNo5.StreetNo5.entity.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PostDto {
    private Long id;
    private String nickname;
    private int viewCount;
    private String content;
    private String imageUrl;
    private String address_name;
    private String tags;
    private String createdDate;
    private String modifiedDate;
    private String music_id;
    private String profileImage;


}
