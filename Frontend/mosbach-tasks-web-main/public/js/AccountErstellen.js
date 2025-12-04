$(document).ready(function() {
  $("#submit").click(function(event) {
    event.preventDefault();

    var loginData = {
      username: $("#username").val(),
      email: $("#email").val(),
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
      url: 'http://localhost:8080/api/register',
      type: 'POST',
      dataType: 'json',
      contentType: 'application/json; charset=utf-8',
      data: JSON.stringify(loginData),
      success: function(response) {
        console.log("Serverantwort:", response);
        console.log("Registrierung erfolgreich. Weiterleitung zur Bestätigungsseite.");
        window.location.href = 'RegistBestätigung.html';
      },
      error: function(xhr, ajaxOptions, thrownError) {
        console.log('Fehlerstatus: ' + xhr.status);
        console.log('Fehlerdetails:', thrownError);
        alert('Ein Fehler ist bei der Registrierung aufgetreten: ' + xhr.status + ' ' + thrownError);
      }
    });
  });
});