package today.seasoning.seasoning.friendship.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import today.seasoning.seasoning.common.enums.LoginType;
import today.seasoning.seasoning.common.exception.CustomException;
import today.seasoning.seasoning.friendship.domain.FriendRequestRepository;
import today.seasoning.seasoning.notification.service.NotificationService;
import today.seasoning.seasoning.user.domain.User;
import today.seasoning.seasoning.user.domain.UserRepository;

@DisplayName("친구 신청 서비스")
@ExtendWith(MockitoExtension.class)
class RequestFriendshipServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    FriendRequestRepository friendRequestRepository;

    @InjectMocks
    RequestFriendshipService requestFriendshipService;

    User requester = new User("requester", "https://test.com/requester.jpg", "requester@email.com", LoginType.KAKAO);
    User requestee = new User("requestee", "https://test.com/requestee.jpg", "requestee@email.com", LoginType.KAKAO);

    @BeforeEach
    void initUserRepository() {
        given(userRepository.findById(requester.getId())).willReturn(Optional.of(requester));
    }

    @Test
    @DisplayName("성공")
    void success() {
        //given : 상대방이 존재하고, 친구 신청 내역이 없으면
        given(userRepository.findByAccountId(requestee.getAccountId())).willReturn(Optional.of(requestee));
        given(friendRequestRepository.existsByFromUserIdAndToUserId(requester.getId(), requestee.getId()))
            .willReturn(false);

        //when & then : 예외가 발생하지 않는다
        assertDoesNotThrow(() -> requestFriendshipService.doService(requester.getId(), requestee.getAccountId()));
    }

    @Test
    @DisplayName("실패 - 상대방 조회 실패")
    void failedByRequesteeNotFound() {
        //given : 아이디에 해당하는 회원이 없는 경우
        given(userRepository.findByAccountId(requestee.getAccountId())).willReturn(Optional.empty());

        //when & then : Not Found 예외가 발생한다
        assertFailedValidation(requester.getId(), requestee.getAccountId(), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("실패 - 자신에게 친구 신청")
    void failedBySelfRequest() {
        //given : 자신의 아이디로 친구 신청한 경우
        String accountId = requester.getAccountId();

        given(userRepository.findByAccountId(accountId)).willReturn(Optional.of(requester));

        //when & then : Bad Request 예외가 발생한다
        assertFailedValidation(requester.getId(), accountId, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("실패 - 이미 신청함")
    void failedByAlreadyExists() {
        //given : 친구 신청 내역이 존재하는 경우(=이미 신청한 경우)
        given(friendRequestRepository.existsByFromUserIdAndToUserId(requester.getId(), requestee.getId()))
            .willReturn(true);

        given(userRepository.findByAccountId(requestee.getAccountId())).willReturn(Optional.of(requestee));

        //when & then : 409 Conflict 예외가 발생한다
        assertFailedValidation(requester.getId(), requestee.getAccountId(), HttpStatus.CONFLICT);
    }

    private void assertFailedValidation(Long requesterId, String requesteeAccountId, HttpStatus httpStatus) {
        assertThatThrownBy(() -> requestFriendshipService.doService(requesterId, requesteeAccountId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("httpStatus", httpStatus);
    }
}
