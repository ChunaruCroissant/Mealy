$(document).ready(function() {
    const API = `${window.API_BASE}/api`;
    const apiUrl = `${API}/mealplan`;

    function getToken() {
        const token = localStorage.getItem('token');
        console.log('Token retrieved:', token);
        return token;
    }

    function getAuthHeaders() {
        return { 'token': token };
    }

    const token = getToken();
    if (!token) {
        console.warn('Kein Token gefunden');
        alert('Bitte melden Sie sich an, um Rezepte anzuzeigen.');
        return;
    }

    var calendarEl = document.getElementById('calendar');
    var calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'de',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
        },
        selectable: true,
        editable: true,
        events: [],
        dateClick: function(info) {
            $('#mealForm').show();
            $('#selectedDate').text(info.dateStr);

            $('#mealAddForm').off('submit').on('submit', function(e) {
                e.preventDefault();
                const mealTime = $('#mealTime').val();
                const recipeId = $('#meal').val();
                const selectedDate = info.dateStr;

                const nutritionalInfo = {
                    calories: $('#calories').val(),
                    protein: $('#protein').val(),
                    carbs: $('#carbs').val(),
                    fats: $('#fats').val()
                };

                calendar.addEvent({
                    title:  `<div>${recipeId}</div>
                                       <div>Kalorien: ${meal.calories}</div>
                                       <div>Protein: ${meal.protein}g</div>
                                       <div>Kohlenhydrate: ${meal.carbs}g</div>
                                       <div>Fette: ${meal.fats}g</div>`,
                    start: selectedDate,
                    allDay: true,
                    extendedProps: {
                        mealType: mealTime,
                        mealName: recipeId,
                        calories: meal.calories,
                        protein: meal.protein,
                        carbs: meal.carbs,
                        fats: meal.fats
                    }
                });

                $.ajax({
                    url: apiUrl,
                    method: 'POST',
                    headers: { 'token': token },
                    data: JSON.stringify({
                        name: recipeId,
                        day: selectedDate,
                        time: mealTime,
                        calories: meal.calories,
                        protein: meal.protein,
                        carbs: meal.carbs,
                        fats: meal.fats
                    }),
                    contentType: 'application/json',
                    success: function(response) {
                        alert('Mahlzeit erfolgreich gespeichert');
                        $('#mealForm').hide();
                        updateNutritionalInfo(selectedDate, mealTime, recipeId);
                    },
                    error: function(xhr) {
                        const errorMessage = xhr.responseJSON && xhr.responseJSON.reason ? xhr.responseJSON.reason : 'Ein unbekannter Fehler ist aufgetreten.';
                        alert('Fehler beim Hinzufügen der Mahlzeit: ' + errorMessage);
                    }
                });
            });
        },
        eventClick: function(info) {
            if (confirm("Möchten Sie diese Mahlzeit löschen?")) {
                info.event.remove();
                removeMealFromDatabase(info.event.startStr, info.event.extendedProps.mealType);
            }
        }
    });

    function removeMealFromDatabase(day, time) {
        $.ajax({
            url: apiUrl,
            type: 'DELETE',
            headers: { 'token': token },
            data: JSON.stringify({
                day: day,
                time: time
            }),
            contentType: 'application/json',
            success: function(response) {
                alert('Mahlzeit erfolgreich entfernt');
            },
            error: function(xhr) {
                const errorMessage = xhr.responseJSON && xhr.responseJSON.reason ? xhr.responseJSON.reason : 'Ein unbekannter Fehler ist aufgetreten.';
                alert('Fehler beim Entfernen der Mahlzeit: ' + errorMessage);
            }
        });
    }

    function loadMealsFromDatabase() {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            headers: { 'token': token },
            success: function(response) {
                console.log('API Response:', response); // Log the response for debugging

                // Check if response is a string and parse it if necessary
                if (typeof response === 'string') {
                    try {
                        response = JSON.parse(response); // Parse the string to JSON
                    } catch (error) {
                        console.error('Fehler beim Parsen der JSON-Antwort:', error);
                        alert('Fehler: Die Antwort vom Server konnte nicht verarbeitet werden.');
                        return;
                    }
                }

                // Directly access response.meals without re-declaration
                const meals = response.meals;

                if (Array.isArray(meals)) {
                    meals.forEach(meal => {
                        console.log('Adding meal to calendar:', meal);
                        calendar.addEvent({
                            title: `${meal.name},
                            Kalorien: ${meal.calories}</div>
                            <div>Protein: ${meal.protein}g</div>
                            <div>Kohlenhydrate: ${meal.carbs}g</div>
                            <div>Fette: ${meal.fats}g</div>`,
                            start: meal.day + 'T' + meal.time + ':00',
                            allDay: true,
                            extendedProps: {
                                calories: meal.calories,
                                protein: meal.protein,
                                carbs: meal.carbs,
                                fats: meal.fats
                            }
                        });
                    });
                } else {
                    console.error('Unexpected response format: meals property is not an array:', response);
                    alert('Fehler: Die Antwort vom Server hat nicht das erwartete Format.');
                }
            },
            error: function(xhr, status, error) {
                console.error('Fehler beim Laden der Mahlzeiten:', error);
                alert('Fehler beim Laden der Mahlzeiten: ' + error);
            }
        });
    }

    calendar.render();
    loadMealsFromDatabase();
});
