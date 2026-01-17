// Recipe.js
// erstellt Rezept im Backend (JSON) + optionales Bild wird NUR lokal (localStorage) gespeichert.

const API = `${window.API_BASE}/api`;
const IMAGE_STORAGE_KEY = 'mealy_recipe_images';

// Dev-Fallback: niemals ein echtes Token überschreiben
if (!localStorage.getItem('token')) {
  localStorage.setItem('token', '123');
}

function getToken() {
  return localStorage.getItem('token');
}

function loadImagesMap() {
  try {
    return JSON.parse(localStorage.getItem(IMAGE_STORAGE_KEY) || '{}') || {};
  } catch (e) {
    console.error('Fehler beim Lesen der Bild-Daten aus localStorage:', e);
    return {};
  }
}

function storeImageForRecipe(recipeKey, imageDataUrl) {
  if (!recipeKey || !imageDataUrl) return;

  const map = loadImagesMap();
  map[recipeKey] = imageDataUrl;

  try {
    localStorage.setItem(IMAGE_STORAGE_KEY, JSON.stringify(map));
  } catch (e) {
    // QuotaExceededError ist hier der Klassiker
    console.warn('Konnte Bild nicht speichern (localStorage voll?).', e);
    alert('Rezept wurde erstellt, aber das Bild konnte lokal nicht gespeichert werden (Speicher voll).');
  }
}

// Liest Bild ein + skaliert es runter (wichtig für localStorage)
function readAndDownscaleImage(file, {
  maxWidth = 1024,
  maxHeight = 1024,
  mime = 'image/jpeg',
  quality = 0.85,
  maxFileBytes = 6 * 1024 * 1024 // 6MB brutto File-Input Limit
} = {}) {
  return new Promise((resolve, reject) => {
    if (!file) return resolve(null);

    if (!file.type || !file.type.startsWith('image/')) {
      return reject(new Error('Datei ist kein Bild.'));
    }

    if (file.size > maxFileBytes) {
      return reject(new Error('Bild ist zu groß (bitte kleineres Bild wählen).'));
    }

    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Bild konnte nicht gelesen werden.'));
    reader.onload = () => {
      const dataUrl = reader.result;

      const img = new Image();
      img.onerror = () => reject(new Error('Bild konnte nicht geladen werden.'));
      img.onload = () => {
        const w = img.width;
        const h = img.height;

        // Wenn schon klein genug, trotzdem in JPEG umwandeln (spart oft massiv Platz)
        const scale = Math.min(maxWidth / w, maxHeight / h, 1);
        const nw = Math.max(1, Math.round(w * scale));
        const nh = Math.max(1, Math.round(h * scale));

        const canvas = document.createElement('canvas');
        canvas.width = nw;
        canvas.height = nh;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, nw, nh);

        try {
          const out = canvas.toDataURL(mime, quality);
          resolve(out);
        } catch (e) {
          reject(new Error('Bild konnte nicht konvertiert werden.'));
        }
      };

      img.src = dataUrl;
    };

    reader.readAsDataURL(file);
  });
}

$(document).ready(function () {
  $('#recipe-form').on('submit', async function (event) {
    event.preventDefault();

    const token = getToken();
    if (!token) {
      alert('Bitte zuerst einloggen (kein Token gefunden).');
      return;
    }

    const recipeName = $('#recipe-name').val().trim();
    const recipeDescription = $('#recipe-description').val().trim();

    if (!recipeName) {
      alert('Bitte gib einen Rezeptnamen ein.');
      return;
    }

    const ingredients = [];
    $('#ingredient-fields-container .ingredient-fields').each(function () {
      const ingredientName = $(this).find('input[name="ingredient_name[]"]').val();
      const ingredientAmount = $(this).find('input[name="ingredient_amount[]"]').val();
      const ingredientUnit = $(this).find('select[name="ingredient_unit[]"]').val();

      // komplett leere Zeilen ignorieren
      if (!ingredientName && !ingredientAmount) return;

      ingredients.push({
        name: ingredientName,
        unit: ingredientUnit,
        amount: ingredientAmount ? parseFloat(ingredientAmount) : null
      });
    });

    const recipeData = {
      name: recipeName,
      ingredients: ingredients,
      description: recipeDescription
    };

    // Optionales Bild
    let imageDataUrl = null;
    const imageInput = $('#recipe-image')[0];
    const imageFile = imageInput && imageInput.files ? imageInput.files[0] : null;

    if (imageFile) {
      try {
        imageDataUrl = await readAndDownscaleImage(imageFile);
      } catch (e) {
        console.warn('Bild wird ignoriert:', e);
        alert(`Bild wird nicht gespeichert: ${e.message}`);
        imageDataUrl = null;
      }
    }

    $.ajax({
      url: `${API}/recipe`,
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(recipeData),
      headers: { token },
      success: function (data) {
        // Backend liefert aktuell nur String → kein Rezept-ID verfügbar
        // Daher keyen wir (vorerst) nach Rezeptname.
        if (imageDataUrl) {
          storeImageForRecipe(recipeName, imageDataUrl);
        }

        // Alte Logik: Backend antwortet "Recipe successfully created"
        const ok = typeof data === 'string'
            ? data.includes('Recipe successfully created')
            : true;

        if (ok) {
          alert('Rezept erfolgreich erstellt!');
          window.location.href = 'RecipeCollection.html';
        } else {
          alert('Fehler beim Erstellen des Rezepts.');
        }
      },
      error: function (xhr, ajaxOptions, thrownError) {
        console.error('Fehler:', thrownError);
        console.error('Status:', xhr.status);
        console.error('Response Text:', xhr.responseText);

        let responseMessage = 'Ein Fehler ist aufgetreten. Bitte versuche es später erneut.';
        try {
          const responseData = JSON.parse(xhr.responseText);
          if (responseData && responseData.reason) responseMessage = responseData.reason;
        } catch (_) {}

        alert(responseMessage);
      }
    });
  });
});

function addIngredient(button) {
  const newIngredient = `
    <div class="ingredient-fields">
      <input type="text" name="ingredient_name[]" placeholder="Zutat" class="ingredient-input" required>
      <input type="number" name="ingredient_amount[]" placeholder="Menge" class="ingredient-input" required>
      <select name="ingredient_unit[]" class="ingredient-input" required>
        <option value="g">Gramm (g)</option>
        <option value="ml">Milliliter (ml)</option>
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
