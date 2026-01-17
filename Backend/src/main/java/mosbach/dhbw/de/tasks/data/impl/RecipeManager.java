package mosbach.dhbw.de.tasks.data.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mosbach.dhbw.de.tasks.model.*;
import mosbach.dhbw.de.tasks.persistence.entity.IngredientValue;
import mosbach.dhbw.de.tasks.persistence.entity.RecipeEntity;
import mosbach.dhbw.de.tasks.persistence.entity.UserEntity;
import mosbach.dhbw.de.tasks.persistence.repo.RecipeRepository;
import mosbach.dhbw.de.tasks.persistence.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class RecipeManager {

    private final RecipeRepository recipeRepo;
    private final UserRepository userRepo;

    // API-Konstanten (besser per ENV/Properties, aber wir halten es erstmal stabil)
    private static final String API_URL = "https://gustar-io-deutsche-rezepte.p.rapidapi.com/nutrition";
    private static final String API_KEY = "dcd8cb5caemsh334466942801b1cp1fc496jsne3b222ee9475";
    private static final String API_HOST = "gustar-io-deutsche-rezepte.p.rapidapi.com";

    public RecipeManager(RecipeRepository recipeRepo, UserRepository userRepo) {
        this.recipeRepo = recipeRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public void saveRecipe(RecipeConv recipe, UserConv user) {
        UserEntity owner = userRepo.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalStateException("Owner user not found in DB: " + user.getEmail()));

        RecipeEntity e = new RecipeEntity();
        e.setOwner(owner);
        e.setName(recipe.getName());
        e.setDescription(recipe.getDescription());

        // Ingredients -> Entity
        List<IngredientValue> ingValues = new ArrayList<>();
        if (recipe.getIngredients() != null) {
            for (IngredientConv ing : recipe.getIngredients()) {
                IngredientValue v = new IngredientValue();
                v.setName(ing.getName());
                v.setUnit(ing.getUnit());
                v.setAmount(ing.getAmount());
                ingValues.add(v);
            }
        }
        e.setIngredients(ingValues);

        // --- Nutrition API call + store result (optional) ---
        try {
            List<String> names = new ArrayList<>();
            List<Double> amounts = new ArrayList<>();

            if (recipe.getIngredients() != null) {
                for (IngredientConv ing : recipe.getIngredients()) {
                    names.add(ing.getName());

                    double amount = 0.0;
                    if (ing.getAmount() != null) {
                        String normalized = ing.getAmount().trim().replace(",", ".");
                        try {
                            amount = Double.parseDouble(normalized);
                        } catch (NumberFormatException ignored) { }
                    }
                    amounts.add(amount);
                }
            }

            String jsonPayload = generateIngredientString(names, amounts);
            NutritionConv result = sendNutritionRequest(jsonPayload);

            if (result != null) {
                e.setCaloriesKcal(result.getCaloriesKcal());
                e.setTotalFatG(result.getTotalFatG());
                e.setSaturatedFatG(result.getSaturatedFatG());
                e.setCholesterolMg(result.getCholesterolMg());
                e.setSodiumMg(result.getSodiumMg());
                e.setTotalCarbohydratesG(result.getTotalCarbohydratesG());
                e.setDietaryFiberG(result.getDietaryFiberG());
                e.setSugarsG(result.getSugarsG());
                e.setProteinG(result.getProteinG());
            }
        } catch (Exception ex) {
            // IMPORTANT: don’t throw, otherwise you rollback saving the recipe
            System.err.println("Nutrition API failed, saving recipe without nutrition: " + ex.getMessage());
        }

        RecipeEntity saved = recipeRepo.save(e);

        // Update conv like before
        recipe.setId(Math.toIntExact(saved.getId()));
        recipe.setOwner(user.getEmail());
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> readRecipeNames(UserConv user) {
        Map<Integer, String> recipes = new LinkedHashMap<>();
        for (RecipeEntity r : recipeRepo.findByOwner_Email(user.getEmail())) {
            recipes.put(Math.toIntExact(r.getId()), r.getName());
        }
        return recipes;
    }

    @Transactional(readOnly = true)
    public List<Integer> readRecipeIDs(UserConv user) {
        List<Integer> ids = new ArrayList<>();
        for (RecipeEntity r : recipeRepo.findByOwner_Email(user.getEmail())) {
            ids.add(Math.toIntExact(r.getId()));
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public List<String> readRecipeName(UserConv user) {
        List<String> names = new ArrayList<>();
        for (RecipeEntity r : recipeRepo.findByOwner_Email(user.getEmail())) {
            names.add(r.getName());
        }
        return names;
    }

    @Transactional(readOnly = true)
    public RecipeConv readRecipeById(int recipeId) {
        RecipeEntity r = recipeRepo.findById((long) recipeId).orElse(null);
        if (r == null) return null;

        List<IngredientConv> ingredients = new ArrayList<>();
        if (r.getIngredients() != null) {
            for (IngredientValue v : r.getIngredients()) {
                ingredients.add(new IngredientConv(v.getName(), v.getUnit(), v.getAmount()));
            }
        }

        return new RecipeConv(Math.toIntExact(r.getId()), r.getName(), ingredients, r.getDescription());
    }

    @Transactional(readOnly = true)
    public List<String> readRecipeNamesByIds(List<Integer> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return List.of();
        }

        // Convert to Long list, but keep duplicates + order in original list
        List<Long> ids = recipeIds.stream()
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .toList();

        // One DB call (may return in any order!)
        Map<Long, String> nameById = new HashMap<>();
        for (RecipeEntity r : recipeRepo.findAllById(ids)) {
            nameById.put(r.getId(), r.getName());
        }

        // Rebuild list in the same order as recipeIds
        List<String> names = new ArrayList<>(recipeIds.size());
        for (Integer id : recipeIds) {
            if (id == null) {
                names.add("-"); // empty slot
            } else {
                names.add(nameById.getOrDefault(id.longValue(), "Unbekanntes Rezept"));
            }
        }
        return names;
    }

    @Transactional(readOnly = true)
    public List<String> readRecipeIngredientName(int id) {
        return recipeRepo.findById((long) id)
                .map(r -> r.getIngredients().stream().map(IngredientValue::getName).toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<Double> readRecipeIngredientAmount(int id) {
        return recipeRepo.findById((long) id)
                .map(r -> r.getIngredients().stream().map(v -> {
                    try { return Double.parseDouble(v.getAmount()); }
                    catch (Exception ignored) { return 0.0; }
                }).toList())
                .orElse(List.of());
    }

    // ---- Unverändert: Helper für Nutrition API ----

    public String generateIngredientString(List<String> names, List<Double> amounts) {
        if (names.size() != amounts.size()) {
            throw new IllegalArgumentException("Beide Listen müssen die gleiche Länge haben.");
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"ingredients\":[");

        for (int i = 0; i < names.size(); i++) {
            jsonBuilder.append("{")
                    .append("\"name\":\"").append(names.get(i)).append("\",")
                    .append("\"amount\":").append(amounts.get(i)).append(",")
                    .append("\"unit\":\"grams\"")
                    .append("}");

            if (i < names.size() - 1) jsonBuilder.append(",");
        }

        jsonBuilder.append("],\"portions\":1}");
        return jsonBuilder.toString();
    }

    public NutritionConv sendNutritionRequest(String ingredientsJson) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("x-rapidapi-key", API_KEY);
            connection.setRequestProperty("x-rapidapi-host", API_HOST);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeBytes(ingredientsJson);
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    ObjectMapper objectMapper = new ObjectMapper();
                    NutritionAnswer nutritionData = objectMapper.readValue(response.toString(), NutritionAnswer.class);
                    return nutritionData.getNutritionalValues();
                }
            } else {
                Logger.getLogger(RecipeManager.class.getName()).log(Level.SEVERE, "Fehlerhafte API-Antwort: Code " + responseCode);
                return null;
            }
        } catch (IOException e) {
            Logger.getLogger(RecipeManager.class.getName()).log(Level.SEVERE, "Verbindungsfehler zur Nährwert-API", e);
            return null;
        }
    }
}
