$(document).ready(function() {

    const token = localStorage.getItem('authToken');
    if (!token) {
        alert('Bitte melde dich zuerst an.');
        return;
    }

    fetchPlans();

    $("#create-folder-button").click(function(event) {
        event.preventDefault();
        createFolder();
    });

    $("#login-button").click(function(event) {
        event.preventDefault();
        const email = $("#email").val();
        const password = $("#password").val();
        loginUser(email, password);
    });
});

function fetchPlans() {
    const token = getToken();
    $.ajax({
        url: 'http://localhost:8080/plan',
        type: 'GET',
        contentType: 'application/json',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        success: function(data) {
            if (data.plans) {
                displayPlans(data.plans);
            } else {
                alert(data.reason || 'Fehler beim Abrufen der Rezepte.');
            }
        },
        error: function(xhr, ajaxOptions, thrownError) {
            console.error('Fehler:', thrownError);
            alert('Ein Fehler ist aufgetreten. Bitte versuche es später erneut.');
        }
    });
}

function createFolder() {
    const folderName = document.getElementById('folder-name').value;
    if (!folderName) {
        alert('Bitte gib einen Ordnernamen ein.');
        return;
    }

    const token = getToken();
    $.ajax({
        url: 'http://localhost:8080/plan',
        type: 'POST',
        contentType: 'application/json',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        data: JSON.stringify({
            name: folderName,
            diet: 'Standard',
            description: 'Dieser Ordner enthält Rezepte.'
        }),
        success: function(result) {
            if (result.message === "Plan successfully created") {
                alert('Ordner erfolgreich erstellt!');
                fetchPlans();
            } else {
                alert(result.reason || 'Fehler beim Erstellen des Ordners.');
            }
        },
        error: function(xhr, ajaxOptions, thrownError) {
            console.error('Fehler:', thrownError);
            alert('Ein Fehler ist aufgetreten. Bitte versuche es später erneut.');
        }
    });
}


function displayPlans(plans) {
    const folderList = document.getElementById('folder-list');
    folderList.innerHTML = '';

    plans.forEach(plan => {
        const folderElement = document.createElement('div');
        folderElement.classList.add('folder');
        folderElement.innerHTML = `
            <h3>${plan.name}</h3>
            <p>${plan.description}</p>
            <button onclick="deleteFolder('${plan.plan_id}')">Ordner löschen</button>
            <button onclick="editFolder('${plan.plan_id}', '${plan.name}')">Ordner bearbeiten</button>
        `;
        folderList.appendChild(folderElement);
    });
}


function deleteFolder(planId) {
    const token = getToken();

    $.ajax({
        url: `http://localhost:8080/plan/${planId}`, // Korrekte Verwendung von Template-Strings
        type: 'DELETE',
        contentType: 'application/json',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        success: function(result) {
            if (result.message === "Plan successfully deleted") {
                alert('Ordner erfolgreich gelöscht');
                fetchPlans();
            } else {
                alert(result.reason || 'Fehler beim Löschen des Ordners');
            }
        },
        error: function(xhr, ajaxOptions, thrownError) {
            console.error('Fehler:', thrownError);
            alert('Ein Fehler ist aufgetreten. Bitte versuche es später erneut.');
        }
    });
}

function editFolder(planId, currentName) {
    const newName = prompt('Geben Sie den neuen Namen des Ordners ein', currentName);
    if (!newName) return;

    const token = getToken();

    $.ajax({
        url: 'http://localhost:8080/plan/${planId}',
        type: 'PUT',
        contentType: 'application/json',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        data: JSON.stringify({
            field: 'name',
            value: newName
        }),
        success: function(result) {
            if (result.message === "plan successfully changed") {
                alert('Ordnername erfolgreich geändert');
                fetchPlans();
            } else {
                alert(result.reason || 'Fehler beim Ändern des Ordnernamens');
            }
        },
        error: function(xhr, ajaxOptions, thrownError) {
            console.error('Fehler:', thrownError);
            alert('Ein Fehler ist aufgetreten. Bitte versuche es später erneut.');
        }
    });
}

function getToken() {
    return localStorage.getItem('authToken');
}
