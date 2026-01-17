// ../js/SharedRecipes.js
$(document).ready(function () {
  // Keys für LocalStorage
  const SHARED_STORAGE_KEY = "mealy_shared_recipes";
  const IMAGE_STORAGE_KEY = "mealy_recipe_images";
  const RATING_STORAGE_KEY = "mealy_recipe_ratings";

  // Initiales Laden der Rezepte beim Seitenaufruf
  loadSharedRecipes();

  // ==========================================
  // 1. Rezepte laden und anzeigen
  // ==========================================
  function loadSharedRecipes() {
    const $list = $("#shared-list");
    $list.empty();

    const sharedList = getFromStorage(SHARED_STORAGE_KEY) || [];
    const imagesMap = getFromStorage(IMAGE_STORAGE_KEY) || {};
    const allRatings = getFromStorage(RATING_STORAGE_KEY) || {};

    if (sharedList.length === 0) {
      $list.html(
        '<p class="empty-state">Es wurden noch keine Rezepte mit der Community geteilt.</p>'
      );
      return;
    }

    // Alphabetisch sortieren nach Name
    sharedList.sort((a, b) => (a.name || "").localeCompare(b.name || ""));

    sharedList.forEach((recipe) => {
      const imageDataUrl = imagesMap[recipe.name];
      const imageHtml = imageDataUrl
        ? `<img src="${imageDataUrl}" alt="${escapeHtml(
            recipe.name
          )}" class="recipe-image">`
        : `<div class="recipe-image" style="background:#f1f5f9; display:flex; align-items:center; justify-content:center; color:#94a3b8; font-size:0.9rem;">Kein Bild</div>`;

      const recipeRatings = allRatings[recipe.id] || [];
      const avgRating = calculateAverage(recipeRatings);
      const countRating = recipeRatings.length;

      // ✅ Neu: Ganze Karte ist klickbar -> navigiert zur Detailseite
      // ✅ Bewerten-Button bleibt drin, aber stoppt Navigation
      const itemHtml = `
        <div class="recipe-item clickable" data-id="${escapeHtml(String(recipe.id))}">
          ${imageHtml}
          <div class="recipe-info">
            <h2>${escapeHtml(recipe.name)}</h2>
            <p class="recipe-meta">ID: ${escapeHtml(String(recipe.id))}</p>

            <div class="recipe-rating-footer">
              <div class="rating-display">
                <span>★</span> ${avgRating}
                <span class="rating-count">(${countRating})</span>
              </div>

              <button class="rate-btn" data-id="${escapeHtml(
                String(recipe.id)
              )}" data-name="${escapeHtml(recipe.name)}">
                Bewerten
              </button>
            </div>
          </div>
        </div>
      `;

      $list.append(itemHtml);
    });
  }

  // ==========================================
  // 1b) Klick auf Rezept-Karte -> Detailseite
  // ==========================================
  $(document).on("click", ".recipe-item.clickable", function (e) {
    // Wenn auf den Bewerten-Button geklickt wurde: NICHT navigieren
    if ($(e.target).closest(".rate-btn").length) return;

    const id = $(this).data("id");
    if (!id) return;

    window.location.href = `SharedRecipeDetail.html?id=${encodeURIComponent(id)}`;
  });

  // ==========================================
  // 2. Modal Steuerung
  // ==========================================
  $(document).on("click", ".rate-btn", function (e) {
    e.stopPropagation(); // ✅ verhindert, dass der Klick auf die Karte durchgeht

    const id = $(this).data("id");
    const name = $(this).data("name");

    $("#ratingRecipeId").val(id);
    $("#ratingRecipeName").val(name);

    $("#modalRecipeTitle").text(name + " bewerten");
    $("#ratingForm")[0].reset();

    $("#ratingModal").fadeIn(200).css("display", "flex");
  });

  $(".close-modal").click(function () {
    $("#ratingModal").fadeOut(200);
  });

  $(window).click(function (event) {
    if (event.target.id === "ratingModal") {
      $("#ratingModal").fadeOut(200);
    }
  });

  // ==========================================
  // 3. Bewertung Absenden
  // ==========================================
  $("#ratingForm").on("submit", function (e) {
    e.preventDefault();

    const recipeId = $("#ratingRecipeId").val();
    const stars = $('input[name="rating"]:checked').val();
    const comment = $("#ratingComment").val();

    if (!stars) {
      alert("Bitte wähle mindestens einen Stern aus.");
      return;
    }

    let allRatings = getFromStorage(RATING_STORAGE_KEY) || {};
    if (!allRatings[recipeId]) allRatings[recipeId] = [];

    allRatings[recipeId].push({
      stars: parseInt(stars, 10),
      comment: comment,
      date: new Date().toISOString(),
    });

    localStorage.setItem(RATING_STORAGE_KEY, JSON.stringify(allRatings));

    $("#ratingModal").fadeOut(200);
    loadSharedRecipes();
  });

  // ==========================================
  // Hilfsfunktionen
  // ==========================================
  function getFromStorage(key) {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error("Fehler beim LocalStorage Zugriff:", e);
      return null;
    }
  }

  function calculateAverage(ratingsArray) {
    if (!ratingsArray || ratingsArray.length === 0) return "0.0";
    const sum = ratingsArray.reduce((acc, curr) => acc + (curr.stars || 0), 0);
    return (sum / ratingsArray.length).toFixed(1);
  }

  function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }
});
