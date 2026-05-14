package domain.auth.repository;

import domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 이메일 조회용 JPA 리포지토리.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
