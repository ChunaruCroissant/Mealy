package mosbach.dhbw.de.tasks.persistence.repo;

import mosbach.dhbw.de.tasks.persistence.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findByOwner_Email(String email);

    Optional<RecipeEntity> findByIdAndOwner_Email(Long id, String email);

    List<RecipeEntity> findBySharedTrueOrderByIdAsc();
    Optional<RecipeEntity> findByIdAndSharedTrue(Long id);

    long deleteByOwner_Email(String email);
}
