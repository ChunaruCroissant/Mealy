package mosbach.dhbw.de.tasks.persistence.repo;

import mosbach.dhbw.de.tasks.persistence.entity.MealEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MealEntryRepository extends JpaRepository<MealEntryEntity, Long> {
    List<MealEntryEntity> findByOwner_Email(String email);
    List<MealEntryEntity> findByOwner_EmailOrderByIdAsc(String email);
    Optional<MealEntryEntity> findByOwner_EmailAndDayAndTime(String email, String day, String time);

    long deleteByOwner_Email(String email);
    long deleteByOwner_EmailAndDayAndTime(String email, String day, String time);
    long deleteByOwner_EmailAndRecipe_Id(String email, Long recipeId);
    @Query("""
        select m.recipe.id
        from MealEntryEntity m
        where m.owner.email = :email
          and m.recipe is not null
    """)
    List<Long> findRecipeIdsByOwnerEmail(@Param("email") String email);

}
