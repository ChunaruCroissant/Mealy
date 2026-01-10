package mosbach.dhbw.de.tasks.persistence.repo;

import mosbach.dhbw.de.tasks.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findFirstByUserName(String userName);
    boolean existsByEmail(String email);
}
