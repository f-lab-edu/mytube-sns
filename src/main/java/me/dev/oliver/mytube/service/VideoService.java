package me.dev.oliver.mytube.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dev.oliver.mytube.aop.LoginValidation;
import me.dev.oliver.mytube.dto.VideoLikeDto;
import me.dev.oliver.mytube.dto.VideoUploadDto;
import me.dev.oliver.mytube.dto.VideoWatchDto;
import me.dev.oliver.mytube.mapper.VideoMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@literal @RequiredArgsConstructor} : final로 선언된 필드, NonNull 어노테이션을 사용한 필드만을 필요로 하는 생성자를 만듦
 * VideoProperties를 생성자 주입을 선택한 이유는 Value String 변수에 어노테이션만 사용하면 immutaeble(불변성)이 아니라는 문제가 있으므로 생성자로
 * 주입을 함
 * <p>
 * {@literal @Transactional} : AOP 프록시를 통해 활성화 되고 내부에는 PlatformTransactionManager 구현과
 * TransactionInterceptor 를 이용하여 메서드 호출을 중심으로 AOP 프록시를 생성. Transactional 어노테이션을 선언적 트랜잭션이라 함. 사용할 때
 * 속성을 설정하지 않으면 기본으로 설정되어있는 속성들로 설정되어 생략이 가능. 트랜잭션의 동작방식에 영향을 줄수 있는 4가지 속성으로 TransactionAttributes
 * 프로퍼티를 통해 전파(propagation), 격리수준(isolation level), 제한시간(time out), 읽기 전용(read only)을 설정할 수 있음.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoService {

  private final VideoMapper videoMapper;
  private final S3FileUploadService s3FileUploadService;
  private final LoginService loginService;

  /**
   * amazon s3에 동영상 업로드 및 file size는 byte 단위로 저장됨, 동영상 컨텐츠 내의 세부사항 기록 db에 저장.
   *
   * @param multipartFile  게시물 관련 정보를 담은 객체
   */
  @LoginValidation
  public void uploadVideo(MultipartFile multipartFile) {

    String fileName = multipartFile.getOriginalFilename();

    if (fileName.isEmpty()) {
      log.error("uploadVideo 메서드에서 {} file을 불러오던 중 null 값이 확인되었습니다", fileName);
      throw new IllegalArgumentException("서버에서 파일을 불러오지 못하여 에러가 발생했습니다");
    }

    try {
      String fileUrl = s3FileUploadService.upload(multipartFile);
      long fileSize = multipartFile.getSize();

      VideoUploadDto videoUploadDto = VideoUploadDto.builder()
          .fileUrl(fileUrl)
          .fileSize(fileSize)
          .build();
      videoMapper.insertVideo(videoUploadDto);
    } catch (RuntimeException e) {
      log.error("uploadVideo 메서드에서 {} file 처리 중 에러가 발생했습니다. s3 활성화 유무와 관련 정보를 확인하십시오", fileName, e);
      throw new IllegalStateException("서버에서 파일 처리중 예상치 못한 에러가 발생했습니다");
    }
  }

  /**
   * 동영상 파일과 관련된 세부 정보를 업로드.
   *
   * @param videoUploadDto userId, title, detailContents를 받아옴.
   */
  @LoginValidation
  public void uploadDetailInfo(VideoUploadDto videoUploadDto) {

    videoMapper.insertDetailInfo(videoUploadDto);
  }

  /**
   * 동영상 조회 특성상 여러 유저가 접근했을 때 데이터베이스에 데이터 요청 수를 줄이기 위해 caching을 사용.
   * caching 할 때 key는 id를 사용, value는 getVideoInfo() 메서드의 return 값을 사용함.
   * unless = "#result == null"  --> 결과가 null이 아닌 경우만 caching 처리.
   *
   * @param id Video 업로드 리스트 id값
   * @return VideoWatchDto의 필드 변수 데이터 값을 리턴
   */
  @Cacheable(key = "#id", value = "getVideoInfo", unless = "#result == null")
  @LoginValidation
  public VideoWatchDto getVideoInfo(int id) {

    return videoMapper.findVideoInfo(id);
  }

  /**
   * 좋아요, 싫어요 누를 userId와 동영상 videoId 정보 추가. 동영상 보기에서 Login 체크를 했으므로 중복 체크 필요 없음.
   *
   * @param videoLikeDto videoId, userId, LikeType(사용자가 like를 누르면 LIKE, 싫어요를 누르면 DISLIKE) 정보
   * @throws IllegalArgumentException DuplicateKeyException이 아닌 다른 예외처리
   */
  public void addLikeCount(VideoLikeDto videoLikeDto) {
    try {
      videoMapper.insertLike(videoLikeDto);
    } catch (DuplicateKeyException e) {
      cancelLikeCount(videoLikeDto);
    } catch (RuntimeException e) {
      log.error("addLikeCount 메서드에서 예상치 못한 에러가 발생했습니다", e);
      throw new IllegalArgumentException("서버에서 좋아요 처리중 예상치 못한 에러가 발생했습니다");
    }
  }

  /**
   * 좋아요나 싫어요 둘중에 하나만 활성화 가능이라서 취소도 간단히 유니크한 primary key 사용.
   *
   * @param videoLikeDto videoId, userId 정보
   */
    private void cancelLikeCount(VideoLikeDto videoLikeDto) {
    String userId = videoLikeDto.getUserId();

    if(!loginService.getLoginId().equals(userId)) {
      throw new IllegalArgumentException(userId + "님은 이 동영상을 업로드한 회원이 아닙니다");
    }
    videoMapper.deleteLike(videoLikeDto);
  }

}
