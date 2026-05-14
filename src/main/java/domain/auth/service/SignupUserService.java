package domain.auth.service;

import domain.auth.entity.User;
import domain.auth.exception.EmailAlreadyRegisteredException;
import domain.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 회원가입(또는 계정 생성) 시 이메일 인증 완료 여부를 강제하는 예시 서비스.
 * 실제 비밀번호·닉네임 등 필드가 생기면 이 클래스를 확장하면 된다.
 */
@Service
public class SignupUserService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    public SignupUserService(UserRepository userRepository, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * 이메일 인증이 완료된 경우에만 사용자 행을 생성한다.
     * 성공 시 Redis의 인증 완료 플래그를 제거해 동일 상태로 재가입을 막는다.
     */
    @Transactional
    public User registerEmailAfterVerification(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        emailVerificationService.requireEmailVerifiedForSignup(email);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("이미 가입된 이메일입니다.", "EMAIL_ALREADY_REGISTERED");
        }

        User saved = userRepository.save(new User(email));
        emailVerificationService.consumeEmailVerification(email);
        return saved;
    }
}
