$(function () {
  // LocalStorage Keys (wie bei dir)
  const SHARED_STORAGE_KEY = "mealy_shared_recipes";
  const IMAGE_STORAGE_KEY = "mealy_recipe_images";

  // OPTIONAL: wenn du Details über API laden willst (nur GET)
  const API_DETAIL = "http://localhost:8080/api/recipe/detail";

  const id = new URLSearchParams(location.search).get("id");
  if (!id) {
    alert("Rezept-ID nicht gefunden.");
    location.href = "SharedRecipes.html";
    return;
  }

  const getToken = () => localStorage.getItem("token");

  const getFromStorage = (key) => {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error("Fehler beim LocalStorage Zugriff:", e);
      return null;
    }
  };

  const loadImages = () => getFromStorage(IMAGE_STORAGE_KEY) || {};

  const renderRecipe = (r) => {
    $("#recipe-name").text(r?.name || "");
    $("#recipe-description").text(r?.description || "");

    const img = loadImages()[r?.name];
    if (img) {
      $("#recipe-image").attr({ src: img, alt: r.name }).show();
    } else {
      $("#recipe-image").hide();
    }

    $("#ingredient-list").empty();
    (r?.ingredients || []).forEach((i) => {
      $("#ingredient-list").append(`<li>${i.name} (${i.amount} ${i.unit})</li>`);
    });
  };

  const findSharedRecipeById = () => {
    const sharedList = getFromStorage(SHARED_STORAGE_KEY) || [];
    return sharedList.find((x) => String(x.id) === String(id)) || null;
  };

  // 1) Erst aus LocalStorage (Community-Liste) laden
  const fromLocal = findSharedRecipeById();
  if (fromLocal && (fromLocal.description || fromLocal.ingredients)) {
    renderRecipe(fromLocal);
    return;
  }

  // 2) Fallback: GET aus API (nur anzeigen)
  $.ajax({
    url: `${API_DETAIL}/${id}`,
    type: "GET",
    headers: getToken() ? { token: getToken() } : {},
    success: (data) => {
      renderRecipe(data);
    },
    error: (xhr) => {
      console.error("GET Fehler:", xhr.status, xhr.responseText);

      // letzter Fallback: wenn nur name/id vorhanden sind, trotzdem anzeigen
      if (fromLocal) {
        renderRecipe(fromLocal);
      } else {
        alert(`Rezept nicht gefunden (${xhr.status}).`);
        location.href = "SharedRecipes.html";
      }
    },
  });
});
