package mosbach.dhbw.de.tasks.persistence.repo;

import mosbach.dhbw.de.tasks.persistence.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findByOwner_Email(String email);
}
