package com.StreetNo5.StreetNo5.controller;


import com.StreetNo5.StreetNo5.jwt.JwtTokenProvider;
import com.StreetNo5.StreetNo5.redis.UserAlert;
import com.StreetNo5.StreetNo5.entity.User;
import com.StreetNo5.StreetNo5.entity.UserComment;
import com.StreetNo5.StreetNo5.entity.UserPost;
import com.StreetNo5.StreetNo5.entity.dto.request.UserCommentRequestDto;
import com.StreetNo5.StreetNo5.entity.dto.response.UserListCommentsDto;
import com.StreetNo5.StreetNo5.service.CommentService;
import com.StreetNo5.StreetNo5.service.UserPostService;
import com.StreetNo5.StreetNo5.service.UserService;
import com.StreetNo5.StreetNo5.redis.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RequestMapping("/user/board")
@RestController

public class CommentController {
    private final CommentService commentService;
    private final UserPostService userPostService;
    private final UserService userService;
    private final PubSubController pubSubController;
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${profile.image.url}")
    private String profileImage;

    @Operation(summary = "댓글 작성 API")
    @PostMapping("/comment/write")
    public UserListCommentsDto write_comment(UserCommentRequestDto userCommentRequestDto, HttpServletRequest httpServletRequest){
        String token = jwtTokenProvider.resolveToken(httpServletRequest);
        UserComment userComment = new UserComment();
        userComment.setContent(userCommentRequestDto.getContent());
        UserPost userPost = userPostService.getUserPost(userCommentRequestDto.getPostId());
        String nickname = getUserNicknameFromJwtToken(token);
        userComment.setNickname(nickname);
        userPost.addComment(userComment);

        Optional<User> user = userService.findUser(nickname);
        User user1 = user.get();
        user1.addComment(userComment);
        userComment.setUser(user1);
        userComment.setProfileImage(profileImage);
        commentService.write_comment(userComment);
        List<UserComment> userComments = userPost.getUserComments();
        // 다른사람의 댓글만 알림
        if (!userPost.getUser().getNickname().equals(userComment.getNickname())) {
            pubSubController.pushMessage(userPost.getUser().getNickname(), "댓글이 등록되었습니다.", userComment.getCreatedDate(), userComment.getContent());
            redisService.saveUserAlert(UserAlert.builder()
                    .createdDate(userComment.getCreatedDate())
                    .title("댓글이 등록되었습니다.")
                    .userId(userPost.getUser().getId())
                    .message(userComment.getContent())
                    .build());
        }
        UserListCommentsDto userListCommentsDto = new UserListCommentsDto();
        userListCommentsDto.setCommentsDto(userComments);
        userListCommentsDto.setNumberOfComments(userComments.size());
        return userListCommentsDto;
    }
    private String getUserNicknameFromJwtToken(String token) {
        Base64.Decoder decoder = Base64.getDecoder();
        final String[] splitJwt = token.split("\\.");
        final String payloadStr = new String(decoder.decode(splitJwt[1].getBytes()));
        String nickname = payloadStr.split(":")[1].replace("\"", "").split(",")[0];
        return nickname;
    }

}
