$(function () {
    // --- Guards ---
    if (!window.API_BASE) {
        console.error("window.API_BASE ist nicht gesetzt. Fehlt config.js im HTML?");
        alert("Konfigurationsfehler: API_BASE fehlt (config.js nicht geladen).");
        return;
    }

    const API = `${window.API_BASE}/api`;
    const DETAIL_URL = `${API}/recipe/detail`;
    const IMG_KEY = "mealy_recipe_images";

    const id = new URLSearchParams(location.search).get("id");
    if (!id) {
        alert("Rezept-ID nicht gefunden.");
        location.href = "RecipeCollection.html";
        return;
    }

    const getToken = () => localStorage.getItem("token");

    const loadImages = () => {
        try {
            return JSON.parse(localStorage.getItem(IMG_KEY) || "{}") || {};
        } catch (e) {
            console.warn("Konnte Bild-Map nicht lesen:", e);
            return {};
        }
    };

    const removeImageByName = (recipeName) => {
        if (!recipeName) return;
        const map = loadImages();
        if (!map[recipeName]) return;
        delete map[recipeName];
        try {
            localStorage.setItem(IMG_KEY, JSON.stringify(map));
        } catch (e) {
            console.warn("Konnte Bild-Map nicht schreiben:", e);
        }
    };

    let currentRecipeName = null;

    const showRecipe = (r) => {
        currentRecipeName = r?.name || null;

        $("#recipe-name").text(r?.name || "");
        $("#recipe-description").text(r?.description || "");

        // Bild anzeigen (nur wenn <img id="recipe-image"> existiert)
        const $img = $("#recipe-image");
        if ($img.length) {
            const map = loadImages();

            // Primär: nach Name
            // Fallbacks: nach ID
            const imgData =
                (r && map[String(r.id)]) ||
                map[String(id)] ||
                (r && map[r.name]) ||
                null;

            if (imgData) {
                $img.attr({ src: imgData, alt: r?.name || "Rezeptbild" }).show();
            } else {
                $img.hide();
            }
        }

        $("#ingredient-list").empty();
        (r?.ingredients || []).forEach((i) => {
            $("#ingredient-list").append(
                `<li>${i.name} (${i.amount} ${i.unit})</li>`
            );
        });
    };

    // --- GET Recipe ---
    const token = getToken();
    if (!token) {
        alert("Kein Token gefunden. Bitte einloggen.");
        location.href = "Login.html";
        return;
    }

    $.ajax({
        url: `${DETAIL_URL}/${id}`,
        type: "GET",
        headers: { token },
        success: showRecipe,
        error: (xhr) => {
            console.error("GET Fehler:", xhr.status, xhr.responseText);
            alert("Fehler beim Abrufen des Rezepts.");
            location.href = "RecipeCollection.html";
        },
    });

    // --- DELETE Recipe ---
    $("#delete-recipe-btn").on("click", (e) => {
        e.preventDefault();

        if (!confirm("Möchten Sie dieses Rezept wirklich löschen?")) return;

        $.ajax({
            url: `${DETAIL_URL}/${id}`,
            type: "DELETE",
            headers: { token: getToken() },
            success: () => {
                // Optional: lokales Bild entfernen
                removeImageByName(currentRecipeName);

                alert("Rezept gelöscht!");
                location.href = "RecipeCollection.html";
            },
            error: (xhr) => {
                console.error("DELETE Fehler:", xhr.status, xhr.responseText);
                alert(`Löschen fehlgeschlagen (${xhr.status}).`);
            },
        });
    });
});
