package mosbach.dhbw.de.tasks.data.impl;

import mosbach.dhbw.de.tasks.model.MealplanConv;
import mosbach.dhbw.de.tasks.model.TimeConv;
import mosbach.dhbw.de.tasks.model.UserConv;
import mosbach.dhbw.de.tasks.persistence.entity.MealEntryEntity;
import mosbach.dhbw.de.tasks.persistence.entity.RecipeEntity;
import mosbach.dhbw.de.tasks.persistence.entity.UserEntity;
import mosbach.dhbw.de.tasks.persistence.repo.MealEntryRepository;
import mosbach.dhbw.de.tasks.persistence.repo.RecipeRepository;
import mosbach.dhbw.de.tasks.persistence.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MealManager {

    private final MealEntryRepository mealRepo;
    private final UserRepository userRepo;
    private final RecipeRepository recipeRepo;

    public MealManager(MealEntryRepository mealRepo, UserRepository userRepo, RecipeRepository recipeRepo) {
        this.mealRepo = mealRepo;
        this.userRepo = userRepo;
        this.recipeRepo = recipeRepo;
    }

    @Transactional(readOnly = true)
    public List<MealplanConv> readAllMeals() {
        List<MealplanConv> out = new ArrayList<>();
        for (MealEntryEntity e : mealRepo.findAll()) {
            String recipeId = e.getRecipe() != null ? String.valueOf(e.getRecipe().getId()) : null;
            out.add(new MealplanConv(
                    Math.toIntExact(e.getId()),
                    e.getOwner().getEmail(),
                    e.getDay(),
                    e.getTime(),
                    recipeId
            ));
        }
        return out;
    }

    @Transactional
    public void saveMeals(MealplanConv meal, UserConv user) {
        UserEntity owner = userRepo.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalStateException("Owner user not found in DB: " + user.getEmail()));

        // Upsert pro Slot (owner+day+time)
        MealEntryEntity entry = mealRepo
                .findByOwner_EmailAndDayAndTime(owner.getEmail(), meal.getDay(), meal.getTime())
                .orElseGet(MealEntryEntity::new);

        entry.setOwner(owner);
        entry.setDay(meal.getDay());
        entry.setTime(meal.getTime());

        RecipeEntity recipe = null;
        try {
            if (meal.getId() != null) {
                long rid = Long.parseLong(meal.getId());
                recipe = recipeRepo.findById(rid).orElse(null);
            }
        } catch (Exception ignored) { }

        entry.setRecipe(recipe);

        MealEntryEntity saved = mealRepo.save(entry);

        // Conv zurück-aktualisieren (wie vorher ID gesetzt wurde)
        meal.setMealId(Math.toIntExact(saved.getId()));
        meal.setOwner(owner.getEmail());
    }

    @Transactional(readOnly = true)
    public List<TimeConv> readTime(UserConv user) {
        List<TimeConv> out = new ArrayList<>();
        for (MealEntryEntity e : mealRepo.findByOwner_Email(user.getEmail())) {
            out.add(new TimeConv(e.getDay(), e.getTime()));
        }
        return out;
    }
}
