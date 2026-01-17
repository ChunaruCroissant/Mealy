package mosbach.dhbw.de.tasks.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe")
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    // Whether this recipe is publicly shared (community page)
    @Column(nullable = false)
    private boolean shared = false;

    // --- Nutrition (nullable, because API may fail / not present) ---
    @Column(name = "calories_kcal")
    private Double caloriesKcal;

    @Column(name = "total_fat_g")
    private Double totalFatG;

    @Column(name = "saturated_fat_g")
    private Double saturatedFatG;

    @Column(name = "cholesterol_mg")
    private Double cholesterolMg;

    @Column(name = "sodium_mg")
    private Double sodiumMg;

    @Column(name = "total_carbohydrates_g")
    private Double totalCarbohydratesG;

    @Column(name = "dietary_fiber_g")
    private Double dietaryFiberG;

    @Column(name = "sugars_g")
    private Double sugarsG;

    @Column(name = "protein_g")
    private Double proteinG;

    @ElementCollection
    @CollectionTable(
            name = "recipe_ingredient",
            joinColumns = @JoinColumn(name = "recipe_id")
    )
    private List<IngredientValue> ingredients = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<IngredientValue> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientValue> ingredients) {
        this.ingredients = ingredients;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public Double getCaloriesKcal() { return caloriesKcal; }
    public void setCaloriesKcal(Double caloriesKcal) { this.caloriesKcal = caloriesKcal; }

    public Double getTotalFatG() { return totalFatG; }
    public void setTotalFatG(Double totalFatG) { this.totalFatG = totalFatG; }

    public Double getSaturatedFatG() { return saturatedFatG; }
    public void setSaturatedFatG(Double saturatedFatG) { this.saturatedFatG = saturatedFatG; }

    public Double getCholesterolMg() { return cholesterolMg; }
    public void setCholesterolMg(Double cholesterolMg) { this.cholesterolMg = cholesterolMg; }

    public Double getSodiumMg() { return sodiumMg; }
    public void setSodiumMg(Double sodiumMg) { this.sodiumMg = sodiumMg; }

    public Double getTotalCarbohydratesG() { return totalCarbohydratesG; }
    public void setTotalCarbohydratesG(Double totalCarbohydratesG) { this.totalCarbohydratesG = totalCarbohydratesG; }

    public Double getDietaryFiberG() { return dietaryFiberG; }
    public void setDietaryFiberG(Double dietaryFiberG) { this.dietaryFiberG = dietaryFiberG; }

    public Double getSugarsG() { return sugarsG; }
    public void setSugarsG(Double sugarsG) { this.sugarsG = sugarsG; }

    public Double getProteinG() { return proteinG; }
    public void setProteinG(Double proteinG) { this.proteinG = proteinG; }
}
