package me.dev.oliver.mytube.controller;

import javax.validation.Valid;
import lombok.AllArgsConstructor;
import me.dev.oliver.mytube.dto.VideoLikeDto;
import me.dev.oliver.mytube.dto.VideoUploadDto;
import me.dev.oliver.mytube.dto.VideoWatchDto;
import me.dev.oliver.mytube.service.VideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
@RequestMapping("video-contents")
public class VideoController {

  private final VideoService videoService;

  /**
   * 동영상 업로드
   * 주의) uploadVideoInfo 메서드와 같이 사용해야함.
   *
   * @param multipartFile 동영상 파일을 및 userId, title, detail contents를 받아옴.
   */
  @PostMapping
  public void uploadVideo(@RequestParam("fileVideo") MultipartFile multipartFile) {

    videoService.uploadVideo(multipartFile);
  }

  /**
   * 동영상에 필요한 정보들을 업로드.
   * 주의) uploadVideo 메서드와 같이 사용해야함.
   *
   * @param videoUploadDto userId, title, detailContents를 받아옴.
   */
  @PostMapping("infos")
  public void uploadVideoInfo(@Valid @RequestBody VideoUploadDto videoUploadDto) {

    videoService.uploadDetailInfo(videoUploadDto);
  }

  /**
   * 동영상을 보기 위한 컨텐츠를 모두 가져옴
   *
   * @param id DB에서 동영상 넘버링한 값을 넣어줌
   * @return 비디오를 시청하기 위한 정보들을 리턴
   */
  @GetMapping("{id}")
  public VideoWatchDto getVideoInfo(@PathVariable int id) {

    return videoService.getVideoInfo(id);
  }

  /**
   * 좋아요, 싫어요를 누르지 않았다면 좋아요, 싫어요 정보 추가. 단, 두가지중에 한가지만 가능 -> 한번에 좋아요, 싫어요 기능 둘다 클릭 안됌.
   *
   * @param videoLikeDto videoId, userId, LikeType(사용자가 like를 누르면 LIKE, 싫어요를 누르면 DISLIKE)를 입력 받음
   */
  @PostMapping("like-tastes")
  public void addLikeCount(@RequestBody VideoLikeDto videoLikeDto) {

    videoService.addLikeCount(videoLikeDto);
  }

}
