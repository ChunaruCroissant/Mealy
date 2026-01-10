package mosbach.dhbw.de.tasks.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "meal_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_day_time",
                columnNames = {"owner_user_id", "day", "time"}
        )
)
public class MealEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 20)
    private String day;   // exakt wie jetzt ("Monday"/"MON"/whatever)

    @Column(nullable = false, length = 20)
    private String time;  // exakt wie jetzt

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private RecipeEntity recipe; // darf null sein

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public RecipeEntity getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeEntity recipe) {
        this.recipe = recipe;
    }
}
