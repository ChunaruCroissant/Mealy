package mosbach.dhbw.de.tasks.controller;

import mosbach.dhbw.de.tasks.data.api.Task;
import mosbach.dhbw.de.tasks.data.api.TaskManager;
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

    TaskManager taskManager = TaskManagerImpl.getTaskManagerImpl();
    MealPlanConverter mealPlanConverter = MealPlanConverter.getMealPlanConverter();

    public MappingController(RecipeManager recipeManager, UserManager userManger, MealManager mealManager) {
        this.recipeManager = recipeManager;
        this.userManger = userManger;
        this.mealManager = mealManager;
    }

    @PostMapping(
            path = "/register",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> register(@RequestBody UserConv data) {

        if(data.getUserName() != null || data.getEmail() != null|| data.getPassword() != null) {

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
    public ResponseEntity<?> getUser(@RequestHeader("Authorization") String data){
        data="123";
        TokenConv t = new TokenConv(data);

        if (userManger.checkToken(t)==true)
        {
            return ResponseEntity.ok(userManger.TokenToUser(data));
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong token"));

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
        data="123";
        TokenConv t = new TokenConv(data);

        if (userManger.checkToken(t)==true)
        {
            return ResponseEntity.ok(recipeManager.readRecipeNames(userManger.TokenToUser(data)));
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));

    }

    @GetMapping("recipe/detail/{id}")
    public ResponseEntity<?> getRecipeById( @PathVariable int id, @RequestHeader("token") String data) {
        data="123";
        TokenConv t = new TokenConv(data);

        if (userManger.checkToken(t)==true) {
            // Je nach Wert der ID eine unterschiedliche Antwort
            return ResponseEntity.ok(recipeManager.readRecipeById(id));
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));

    }


    @GetMapping("/mealplan")
    public ResponseEntity<?> getMeals(@RequestHeader("token") String data) {
        try {
            data = "123"; // Zum Testen, später durch den echten Token ersetzen
            TokenConv t = new TokenConv(data);

            if (!userManger.checkToken(t)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Ungültiges Token"));
            }

            UserConv user = userManger.TokenToUser(data);
            if (user == null) {
                Logger.getLogger(MealManager.class.getName()).log(Level.SEVERE, "Fehler: Benutzer für Token nicht gefunden");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Ungültiges Token oder Benutzer nicht gefunden.");
            }

            List<Integer> RecipeIDS = recipeManager.readRecipeIDs(user);
            List<String> RecipeNames = recipeManager.readRecipeName(user);
            List<TimeConv> MealTimes = mealManager.readTime(user);
            List<SendNutriConv> NutritionValues = new ArrayList<>();

            if (RecipeIDS.isEmpty() || RecipeNames.isEmpty() || MealTimes.isEmpty()) {
                Logger.getLogger(MealManager.class.getName()).log(Level.INFO, "Fehler: Keine Daten für Benutzer vorhanden.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Es sind keine Daten für diesen Benutzer vorhanden.");
            }

            // Nutri-Werte anfragen und prüfen, ob alle erfolgreich geladen wurden
            for (int id : RecipeIDS) {
                try {
                    NutritionConv nutris = recipeManager.sendNutritionRequest(
                            recipeManager.generateIngredientString(
                                    recipeManager.readRecipeIngredientName(id),
                                    recipeManager.readRecipeIngredientAmount(id)
                            )

                    );
                    System.out.println(nutris.toString());
                    if (nutris == null) {
                        Logger.getLogger(RecipeManager.class.getName()).log(Level.SEVERE, "Nährwertdaten fehlen für Rezept-ID: " + id);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fehler: Die Nährwertdaten konnten nicht vollständig geladen werden.");
                    }
                    NutritionValues.add(new SendNutriConv(nutris.getCaloriesKcal(), nutris.getProteinG(), nutris.getTotalCarbohydratesG(), nutris.getTotalFatG()));
                } catch (Exception e) {
                    Logger.getLogger(RecipeManager.class.getName()).log(Level.SEVERE, "Fehler beim Laden der Nährwerte für Rezept-ID: " + id, e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Laden der Nährwertdaten.");
                }
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

        token="123";
        TokenConv t = new TokenConv(token);

        if (userManger.checkToken(t)==true)
        {
            mealManager.saveMeals(recipe, userManger.TokenToUser(token));
            return ResponseEntity.ok("Recipe successfully added to meal plan");
        }
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("reason", "Wrong Token"));
    }

    //Hardwig Stuff...

    @GetMapping("/tasks")
    public SortedTasks getAllTasks(
            @RequestParam(value = "sortOrder", defaultValue = "date") String sortOrder,
            @RequestParam(value = "token", defaultValue = "no-token") String token
        )
    {
        // TODO: Check the token with your TokenManager

        Logger
                .getLogger("MappingController")
                .log(Level.INFO, "Bin drin");

        SortedTasks answerSortedTasks = new SortedTasks();

        List<mosbach.dhbw.de.tasks.model.Task> myTasks = new ArrayList<>();
        for(Task t : taskManager.getAllTasks())
            myTasks.add(new mosbach.dhbw.de.tasks.model.Task(
                    t.getModule(),
                    t.getGrade(),
                    t.getDateAsNumber()
            ));

        // TODO: Sort the tasks, at the moment not sorted
        answerSortedTasks.setSortOrder("NOT YET SORTED");

        Logger
                .getLogger("MappingController")
                .log(Level.INFO, "Tasks from file");

        answerSortedTasks.setTasks(myTasks);
        return
                answerSortedTasks;
    }

    @PostMapping(
            path = "/tasks",
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public MessageAnswer createTask(@RequestBody TokenTask tokenTask) {

        // TODO Check the token with your TokenManager

        Logger
                .getLogger("MappingController")
                .log(Level.INFO, "Bin drin");

        // TODO Replace the studentId with the real StudentId that you get from the TokenManager
        Task t = new TaskImpl(
                tokenTask.getTask().getModule(),
                tokenTask.getTask().getGrade(),
                tokenTask.getTask().getDateAsNumber(),
                "55");
        taskManager.addTask(t);

        String answer = "You were pretty lazy.";
        if (tokenTask.getTask().getGrade() < 2.5)
            answer = "You learned. But you could learn more.";

        return
                new MessageAnswer(answer);
    }

    @GetMapping("/create-task-table")
    public String createDBTable(@RequestParam(value = "token", defaultValue = "no-token") String token) {
        Logger.getLogger("MappingController")
                .log(Level.INFO,"MappingController create-task-table " + token);

        // TODO:  Check token, this should be a very long, super secret token
        // Usually this is done via a different, internal component, not the same component for all public REST access

        taskManager.createTaskTable();

        return "ok";
    }
}