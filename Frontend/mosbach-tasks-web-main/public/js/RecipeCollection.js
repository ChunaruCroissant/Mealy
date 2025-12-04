$(document).ready(function() {
    const apiUrl = 'http://localhost:8080/api/collection';

    function getToken() {
        const token = localStorage.getItem('token');
        console.log('Token retrieved:', token);
        return token;
    }

    const token = getToken();
    if (token) {
        fetchRecipes();
    } else {
        console.warn('Kein Token gefunden');
        alert('Bitte melden Sie sich an, um Rezepte anzuzeigen.');
    }

    $('#add-recipe-btn').on('click', function() {
        window.location.href = 'RecipeCreate.html';
    });

    function fetchRecipes() {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            headers: { 'token': token },
            success: function(response) {
                console.log('Rezepte erfolgreich abgerufen:', response);

                const recipesArray = Object.keys(response).map(key => ({
                    plan_id: key,
                    name: response[key]
                }));

                if (recipesArray.length > 0) {
                    displayRecipes(recipesArray);
                } else {
                    console.warn('Keine Rezepte im Antwort-Objekt gefunden.');
                    alert('Keine Rezepte verfügbar.');
                }
            },
            error: function(xhr) {
                console.error('Fehler beim Abrufen der Rezepte:', xhr);
                alert('Ein Fehler ist aufgetreten. Bitte versuche es später erneut.');
            }
        });
    }

    function displayRecipes(recipes) {
        $('#recipe-list').empty();
        recipes.sort((a, b) => a.name.localeCompare(b.name));

        recipes.forEach(recipe => {
            const recipeItem = `<div class="recipe-item" data-id="${recipe.plan_id}">
                <input type="checkbox" class="select-recipe-checkbox">
                <span class="recipe-name">${recipe.name}</span>
            </div>`;
            $('#recipe-list').append(recipeItem);
        });

        $('.recipe-item').on('click', function() {
            const recipeId = $(this).data('id');
             console.log('Rezept-ID:', recipeId);
            window.location.href = `RecipeDetail.html?id=${recipeId}`;
        });
    }
});
