package mosbach.dhbw.de.tasks.controller;

import mosbach.dhbw.de.tasks.data.impl.*;
import mosbach.dhbw.de.tasks.model.*;
import mosbach.dhbw.de.tasks.data.basis.User;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api")
public class MappingController {

    private final RecipeManager recipeManager;
    private final UserManager userManger;
    private final MealManager mealManager;

    MealPlanConverter mealPlanConverter = MealPlanConverter.getMealPlanConverter();

    public MappingController(RecipeManager recipeManager, UserManager userManger, MealManager mealManager) {
        this.recipeManager = recipeManager;
        this.userManger = userManger;
        this.mealManager = mealManager;
    }

    private String extractToken(String tokenHeader, String authorizationHeader) {
        if (tokenHeader != null && !tokenHeader.isBlank()) return tokenHeader;
        if (authorizationHeader == null || authorizationHeader.isBlank()) return null;
        String a = authorizationHeader.trim();
        if (a.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return a.substring(7).trim();
        }
        return a;
    }

    @PostMapping(
            path = "/register",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> register(@RequestBody UserConv data) {

        if(data.getUserName() != null && data.getEmail() != null && data.getPassword() != null) {

            User u = new User(
                    data.getUserName(),
                    data.getEmail(),
                    data.getPassword()
                    );


            userManger.addUser(u);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Account successfully registered"));
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Uncomplete data"));

    }

    @GetMapping("/login")
    public String getServerAlive() {
        return "The Mosbach Task Organiser is alive.";
    }

    @PostMapping(
            path="/login",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> login(@RequestBody UserConv data) {
        if (userManger.checkUser(data)==true)
        {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("token", "123"));
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Account does not exist"));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(
            @RequestHeader(value = "token", required = false) String tokenHeader,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        String token = extractToken(tokenHeader, authorizationHeader);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Missing token"));
        }

        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong token"));
        }

        UserConv u = userManger.TokenToUser(token);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "User not found"));
        }
        // Do not return password hash
        u.setPassword(null);
        return ResponseEntity.ok(u);
    }

    @PutMapping(
            path = "/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> updateUser(
            @RequestHeader(value = "token", required = false) String tokenHeader,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody UserConv update) {

        String token = extractToken(tokenHeader, authorizationHeader);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Missing token"));
        }

        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong token"));
        }

        UserConv owner = userManger.TokenToUser(token);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "User not found"));
        }

        try {
            boolean ok = userManger.updateUserForTokenOwner(owner, update);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "Update failed"));
            }
            return ResponseEntity.ok(Map.of("message", "Account details successfully changed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", e.getMessage()));
        }
    }

    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "token", required = false) String tokenHeader,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        String token = extractToken(tokenHeader, authorizationHeader);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Missing token"));
        }

        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong token"));
        }

        UserConv owner = userManger.TokenToUser(token);
        if (owner == null || owner.getEmail() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "User not found"));
        }

        // Delete dependent data first (FK constraints)
        mealManager.deleteMealsByUserEmail(owner.getEmail());
        recipeManager.deleteRecipesByUserEmail(owner.getEmail());

        boolean deleted = userManger.deleteUserByEmail(owner.getEmail());
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Account successfully deleted"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "Account could not be deleted"));
    }


    @PostMapping(
            path = "/recipe",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> saveRecipe(
            @RequestHeader("token") String token,
            @RequestBody RecipeConv recipe) {

        TokenConv t = new TokenConv(token);

        // Überprüfen des Tokens
        if (userManger.checkToken(t)) {
            // Rezept speichern
            recipeManager.saveRecipe(recipe, userManger.TokenToUser(token));
            return ResponseEntity.ok("Recipe successfully created");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong token"));
        }
    }

    @GetMapping("/collection")
    public ResponseEntity<?> getRecepes(@RequestHeader("token") String data){
        TokenConv t = new TokenConv(data);

        if (userManger.checkToken(t)==true)
        {
            return ResponseEntity.ok(recipeManager.readRecipeNames(userManger.TokenToUser(data)));
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));

    }

    @GetMapping("recipe/detail/{id}")
    public ResponseEntity<?> getRecipeById(@PathVariable int id, @RequestHeader("token") String token) {
        TokenConv t = new TokenConv(token);

        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
        }

        UserConv user = userManger.TokenToUser(token);
        RecipeConv r = recipeManager.readRecipeByIdForOwner(id, user);
        if (r == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("reason", "Recipe not found"));
        }
        return ResponseEntity.ok(r);
    }

    @DeleteMapping("recipe/detail/{id}")
    public ResponseEntity<?> deleteRecipeById(@PathVariable long id, @RequestHeader("token") String token) {
        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
        }

        UserConv user = userManger.TokenToUser(token);
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "User not found"));
        }

        // Clear mealplan references first (FK)
        mealManager.deleteMealsByRecipe(user, id);

        boolean deleted = recipeManager.deleteRecipeOwned(id, user);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Recipe successfully deleted"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("reason", "Recipe not found"));
    }


    @GetMapping("/mealplan")
    public ResponseEntity<?> getMeals(@RequestHeader("token") String data) {
        try {
            TokenConv t = new TokenConv(data);

            if (!userManger.checkToken(t)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Ungültiges Token"));
            }

            UserConv user = userManger.TokenToUser(data);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Benutzer nicht gefunden.");
            }

//            List<Integer> RecipeIDS = recipeManager.readRecipeIDs(user);
//            List<String> RecipeNames = recipeManager.readRecipeName(user);
//            List<TimeConv> MealTimes = mealManager.readTime(user);
//            List<SendNutriConv> NutritionValues = new ArrayList<>();
//
//            if (RecipeIDS.isEmpty() || RecipeNames.isEmpty() || MealTimes.isEmpty()) {
//                Logger.getLogger(MealManager.class.getName()).log(Level.INFO, "Fehler: Keine Daten für Benutzer vorhanden.");
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Es sind keine Daten für diesen Benutzer vorhanden.");
//            }
//
//            // Nutri-Werte anfragen und prüfen, ob alle erfolgreich geladen wurden
//            for (int id : RecipeIDS) {
//                try {
//                    NutritionConv nutris = recipeManager.sendNutritionRequest(
//                            recipeManager.generateIngredientString(
//                                    recipeManager.readRecipeIngredientName(id),
//                                    recipeManager.readRecipeIngredientAmount(id)
//                            )
//
//                    );
//                    System.out.println(nutris.toString());
//                    if (nutris == null) {
//                        Logger.getLogger(RecipeManager.class.getName()).log(Level.SEVERE, "Nährwertdaten fehlen für Rezept-ID: " + id);
//                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Die Nährwertdaten konnten nicht vollständig geladen werden.");
//                    }
//                    NutritionValues.add(new SendNutriConv(nutris.getCaloriesKcal(), nutris.getProteinG(), nutris.getTotalCarbohydratesG(), nutris.getTotalFatG()));
//                } catch (Exception e) {
//                    Logger.getLogger(RecipeManager.class.getName()).log(Level.SEVERE, "Fehler beim Laden der Nährwerte für Rezept-ID: " + id, e);
//                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Laden der Nährwertdaten.");
//                }
//            }
//
//            // Protokollieren der Listenlängen
//            Logger.getLogger(MealPlanConverter.class.getName()).log(Level.INFO, "RecipeNames size: " + RecipeNames.size());
//            Logger.getLogger(MealPlanConverter.class.getName()).log(Level.INFO, "MealTimes size: " + MealTimes.size());
//            Logger.getLogger(MealPlanConverter.class.getName()).log(Level.INFO, "NutritionValues size: " + NutritionValues.size());
//
//            if (RecipeNames.size() != MealTimes.size() || MealTimes.size() != NutritionValues.size()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Die Listenlängen stimmen nicht überein.");
//            }
//
//            return ResponseEntity.ok(mealPlanConverter.convertToMealPlanJson(RecipeNames, MealTimes, NutritionValues));
//
//        } catch (Exception e) {
//            Logger.getLogger(MealManager.class.getName()).log(Level.SEVERE, "Fehler in der /mealplan-Anfrage", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ein unerwarteter Fehler ist aufgetreten.");
//        }
//    }
            List<TimeConv> MealTimes = mealManager.readTime(user);
            List<Integer> RecipeIDS = mealManager.readMealPlanRecipeIds(user);
            List<String> RecipeNames = recipeManager.readRecipeNamesByIds(RecipeIDS);

            //Hier später Datenbank abfragen
            List<SendNutriConv> NutritionValues = new ArrayList<>();
            for (int i = 0; i < RecipeNames.size(); i++) {
                NutritionValues.add(new SendNutriConv(0, 0, 0, 0));
            }

            // Protokollieren der Listenlängen
            Logger.getLogger(MealPlanConverter.class.getName()).log(Level.INFO, "RecipeNames size: " + RecipeNames.size());
            Logger.getLogger(MealPlanConverter.class.getName()).log(Level.INFO, "MealTimes size: " + MealTimes.size());
            Logger.getLogger(MealPlanConverter.class.getName()).log(Level.INFO, "NutritionValues size: " + NutritionValues.size());

            if (RecipeNames.size() != MealTimes.size() || MealTimes.size() != NutritionValues.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Die Listenlängen stimmen nicht überein.");
            }


            return ResponseEntity.ok(mealPlanConverter.convertToMealPlanJson(RecipeNames, MealTimes, NutritionValues));

        } catch (Exception e) {
            Logger.getLogger(MealManager.class.getName()).log(Level.SEVERE, "Fehler in der /mealplan-Anfrage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ein unerwarteter Fehler ist aufgetreten.");
        }
    }


    @PostMapping(
            path = "/mealplan",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> RecipeToMealplan(
            @RequestHeader("token") String token,
            @RequestBody MealplanConv recipe) {

        TokenConv t = new TokenConv(token);

        if (userManger.checkToken(t)==true)
        {
            mealManager.saveMeals(recipe, userManger.TokenToUser(token));
            return ResponseEntity.ok("Recipe successfully added to meal plan");
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
    }

    @DeleteMapping(
            path = "/mealplan",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> deleteMealFromMealplan(
            @RequestHeader("token") String token,
            @RequestBody Map<String, String> payload) {

        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
        }

        UserConv user = userManger.TokenToUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "User not found"));
        }

        String day = payload != null ? payload.get("day") : null;
        String time = payload != null ? payload.get("time") : null;

        boolean deleted = mealManager.deleteMealSlot(user, day, time);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Meal successfully removed"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("reason", "Meal entry not found"));
    }

    // ---- Recipe sharing (community) ----

    @GetMapping("/shared-recipes")
    public ResponseEntity<?> listSharedRecipes() {
        return ResponseEntity.ok(recipeManager.listSharedRecipes());
    }

    @GetMapping("/shared-recipes/{id}")
    public ResponseEntity<?> getSharedRecipe(@PathVariable long id) {
        RecipeConv r = recipeManager.readSharedRecipeById(id);
        if (r == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("reason", "Shared recipe not found"));
        }
        return ResponseEntity.ok(r);
    }

    @PostMapping("/recipe/{id}/share")
    public ResponseEntity<?> shareRecipe(@PathVariable long id, @RequestHeader("token") String token) {
        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
        }
        UserConv user = userManger.TokenToUser(token);
        boolean ok = recipeManager.setRecipeShared(id, user, true);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("reason", "Recipe not found"));
        }
        return ResponseEntity.ok(Map.of("message", "Recipe shared", "shared", true));
    }

    @DeleteMapping("/recipe/{id}/share")
    public ResponseEntity<?> unshareRecipe(@PathVariable long id, @RequestHeader("token") String token) {
        TokenConv t = new TokenConv(token);
        if (!userManger.checkToken(t)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
        }
        UserConv user = userManger.TokenToUser(token);
        boolean ok = recipeManager.setRecipeShared(id, user, false);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("reason", "Recipe not found"));
        }
        return ResponseEntity.ok(Map.of("message", "Recipe unshared", "shared", false));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}