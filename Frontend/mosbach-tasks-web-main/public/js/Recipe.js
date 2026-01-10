
const tokenValue = '123';
localStorage.setItem('token', tokenValue);
console.log('Token set:', tokenValue);

$(document).ready(function() {
  $('#recipe-form').on('submit', function(event) {
    event.preventDefault();

    const token = localStorage.getItem('token');
    console.log('Token retrieved:', token);

    if (!token) {
      alert('Es gibt kein Rezept-Token.');
      return;
    }

    const recipeName = $('#recipe-name').val();
    const recipeDescription = $('#recipe-description').val();
    const ingredients = [];

    $('#ingredient-fields-container .ingredient-fields').each(function() {
      const ingredientName = $(this).find('input[name="ingredient_name[]"]').val();
      const ingredientAmount = $(this).find('input[name="ingredient_amount[]"]').val();
      const ingredientUnit = $(this).find('select[name="ingredient_unit[]"]').val();

      ingredients.push({
        name: ingredientName,
        unit: ingredientUnit,
        amount: parseFloat(ingredientAmount)
      });
    });

    const recipeData = {
      name: recipeName,
      ingredients: ingredients,
      description: recipeDescription
    };

    $.ajax({
      url: 'https://mealybackend-fearless-bushbuck-kc.apps.01.cf.eu01.stackit.cloud/api/recipe',
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(recipeData),
      headers: {
        'token': token
      },
      success: function(data) {
        console.log("Antwort von der API erhalten:", data);

        if (data && JSON.stringify(data).includes("Recipe successfully created")) {
          alert('Rezept erfolgreich erstellt!');
          window.location.href = 'RecipeCollection.html';
        } else {
          console.log("Rezept-Erstellung fehlgeschlagen: ", data.reason || 'Unbekannter Fehler.');
          alert('Fehler beim Erstellen des Rezepts: ' + (data.reason || 'Bitte versuche es später erneut.'));
        }
      },
      error: function(xhr, ajaxOptions, thrownError) {
        console.error('Fehler:', thrownError);
        console.error('Status:', xhr.status);
        console.error('Response Text:', xhr.responseText);

        let responseMessage = 'Ein Fehler ist aufgetreten. Bitte versuche es später erneut.';
        try {
          const responseData = JSON.parse(xhr.responseText);
          if (responseData && responseData.reason) {
            responseMessage = responseData.reason;
          }
        } catch (e) {
          console.error('Fehler beim Parsen der Antwort:', e);
        }

        alert(responseMessage);
    },});
  });
});

function addIngredient(button) {
  const newIngredient = `
    <div class="ingredient-fields">
      <input type="text" name="ingredient_name[]" placeholder="Zutat" class="ingredient-input" required>
      <input type="number" name="ingredient_amount[]" placeholder="Menge" class="ingredient-input" required>
      <select name="ingredient_unit[]" class="ingredient-input" required>
        <option value="g">Gramm (g)</option>
        <option value="kg">Kilogramm (kg)</option>
        <option value="ml">Milliliter (ml)</option>
        <option value="l">Liter (l)</option>
        <option value="stück">Stück</option>
      </select>
      <button type="button" class="add-ingredient-btn" onclick="addIngredient(this)">+</button>
      <button type="button" class="remove-ingredient-btn" onclick="removeIngredient(this)">-</button>
    </div>
  `;
  $('#ingredient-fields-container').append(newIngredient);
}

function removeIngredient(button) {
  $(button).closest('.ingredient-fields').remove();
}
