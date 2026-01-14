$(document).ready(function() {
  $("#submit").click(function(event) {
    event.preventDefault();

    const API = `${window.API_BASE}/api`;

    var loginData = {
      email: $("#email").val(),
      userName: $("#username").val(), //bruh -RL
      password: $("#password").val(),
      passwordConfirm: $("#passwordConfirm").val(),
    };

    console.log("Eingesammelte Daten:", loginData);

    if (loginData.password !== loginData.passwordConfirm) {
      console.log("Passwort und Passwort-Bestätigung stimmen nicht überein.");
      alert('Die Passwörter stimmen nicht überein.');
      return;
    }
    delete loginData.passwordConfirm;

    console.log("Daten, die gesendet werden:", loginData);

    $.ajax({
      url: `${API}/register`,
      type: 'POST',
      dataType: 'json',
      contentType: 'application/json; charset=utf-8',
      data: JSON.stringify(loginData),
      success: function(response) {
        console.log("Serverantwort:", response);
        if (response.message === "Account successfully registered") {
          console.log("Registrierung erfolgreich. Weiterleitung zur Bestätigungsseite.");
          window.location.href = 'RegistBestätigung.html';
        } else {
          console.log("Registrierung fehlgeschlagen:", response.reason);
          alert(response.reason || "Registrierung fehlgeschlagen");
        }
      },
      //Return Error Info to User
      error: function(xhr)  {
        console.log("Fehlerstatus:", xhr.status);
        console.log("Response:", xhr.responseJSON || xhr.responseText);
        const msg = xhr.responseJSON?.reason || xhr.responseText || "Unbekannter Fehler";
        alert("Registrierung fehlgeschlagen: " + msg);
      }
    });
  });
});