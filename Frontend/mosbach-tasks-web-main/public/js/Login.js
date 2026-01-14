$(document).ready(function() {
  $("#submitLogin").click(function(event) {
    event.preventDefault();

    const API = `${window.API_BASE}/api`;

    var loginData = {
      userName: $("#userName").val(),
      password: $("#password").val()
    };

    if (!loginData.userName || !loginData.password) {
      console.log("Fehler: Ein oder mehrere Felder sind leer.");
      alert('Bitte alle Felder ausfüllen.');
      return;
    }

    console.log("Login-Daten gesammelt:", loginData);

    $.ajax({
      url: `${API}/login`,
      type: 'POST',
      dataType: 'json',
      contentType: 'application/json; charset=utf-8',
      data: JSON.stringify(loginData),
      success: function(data) {
        console.log("Antwort von der API erhalten:", data);
        if (data.token) {
          localStorage.setItem('authToken', data.token);
          localStorage.setItem("token", data.token);
          console.log("Token erfolgreich gespeichert:", data.token);

          window.location.href = 'Hompage2.html';
        } else {
          console.log("Login fehlgeschlagen: Kein Token erhalten.");
          alert('Login fehlgeschlagen. Bitte überprüfe deine Zugangsdaten.');
        }
      },
      error: function(xhr, ajaxOptions, thrownError) {
        console.log("Fehler beim Senden der Anfrage:");
        console.log("Statuscode:", xhr.status);
        console.log("Fehlerdetails:", thrownError);
        console.log("Vollständige Antwort:", xhr);
        alert('Ein Fehler ist beim Login aufgetreten. Bitte versuche es erneut.');
      }
    });
  });
});
