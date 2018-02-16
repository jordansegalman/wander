var express = require('express');
var body_parser = require('body-parser');
var mysql = require('mysql');

var app = express();
var json_parser = body_parser.json();

const port = 3000;

// Constants used for verifying JSON subsmission by users
const username = "username";
const password = "password";
const email = "email";
const newUsername = "newUsername";
const newPassword = "newPassword";
const newEmail = "newEmail";
const host = "localhost";

// For actual implementation, we need to set username & password differently
const db_username = "wander";
const db_password = "wander";
const db_name = "wander";
const db_table = "accounts";

// Creates a connection to the MySQL database
var connection = mysql.createConnection({
  //multipleStatements: true,
  host: host,
  user: db_username,
  password: db_password,
  database: db_name
});

app.get('/', function(request, response) {
  response.send("GET request\n");
});

// Called when a POST request is made to /registerAccount
app.post('/registerAccount', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 3 parameters (username, password, and email)
  if (Object.keys(request.body).length != 3 || post_variables[0] !== username || post_variables[1] !== password || post_variables[2] !== email) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var p = request.body.password;
  var e = request.body.email;

  register(u, p, e, response);
});

// Called when a POST request is made to /login
app.post('/login', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 2 parameters (username and password)
  if (Object.keys(request.body).length != 2 || post_variables[0] !== username || post_variables[1] !== password) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var p = request.body.password;

  login(u, p, response);
});

// Called when a POST request is made to /deleteAccount
app.post('/deleteAccount', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 3 parameters (username, password, and email)
  if (Object.keys(request.body).length != 3 || post_variables[0] !== username || post_variables[1] !== password || post_variables[2] !== email) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var p = request.body.password;
  var e = request.body.email;

  deleteAccount(u, p, e, response);
});

// Called when a POST request is made to /changeUsername
app.post('/changeUsername', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 4 parameters (username, password, email, and newUsername)
  if (Object.keys(request.body).length != 4 || post_variables[0] !== username || post_variables[1] !== password || post_variables[2] !== email || post_variables[3] !== newUsername) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var p = request.body.password;
  var e = request.body.email;
  var n = request.body.newUsername;

  changeUsername(u, p, e, n, response);
});

// Called when a POST request is made to /changePassword
app.post('/changePassword', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 4 parameters (username, password, email, and newPassword)
  if (Object.keys(request.body).length != 4 || post_variables[0] !== username || post_variables[1] !== password || post_variables[2] !== email || post_variables[3] !== newPassword) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var p = request.body.password;
  var e = request.body.email;
  var n = request.body.newPassword;

  changePassword(u, p, e, n, response);
});

// Called when a POST request is made to /changeEmail
app.post('/changeEmail', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 4 parameters (username, password, email, and newEmail)
  if (Object.keys(request.body).length != 4 || post_variables[0] !== username || post_variables[1] !== password || post_variables[2] !== email || post_variables[3] !== newEmail) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var p = request.body.password;
  var e = request.body.email;
  var n = request.body.newEmail;

  changeEmail(u, p, e, n, response);
});

// Called when a POST request is made to /forgotPassword
app.post('/forgotPassword', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 2 parameters (username and email)
  if (Object.keys(request.body).length != 2 || post_variables[0] !== username || post_variables[1] !== email) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var e = request.body.email;

  forgotPassword(u, e, response);
});

// Helper function that registers a user if username and email does not already exist
function register(u, p, e, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? OR ??=?";
  var post = [username, db_table, username, u, email, e];
  connection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 0) {
      return response.status(400).send("Username or email already exists! Try again.\n");
    } else {
      var sql = "INSERT INTO ?? SET ?";
      var post = {username: u, password: p, email: e};
      connection.query(sql, [db_table, post], function (err, result) {
        if (err) throw err;
        console.log("User account registered.");
        return response.send("Successfully registered an account.\n");
      });
    }
  });
}

// Helper function that verifies user has an account and logs them in
function login(u, p, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?";
  var post = [username, db_table, username, u, password, p];
  connection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or password. Try again.\n");
    } else {
      console.log("User logged in.");
      return response.status(200).send("Successfully logged in.\n");
    }
  });
}

// Helper function that deletes an account
function deleteAccount(u, p, e, response) {
  var sql = "DELETE FROM ?? WHERE ??=? AND ??=? AND ??=?";
  var post = [db_table, username, u, password, p, email, e];
  connection.query(sql, post, function(err, result) {
    if (err) throw err;
    if (result.affectedRows == 1) {
      console.log("User account deleted.");
      return response.status(200).send("Successfully deleted account.\n");
    } else if (result.affectedRows > 1) {
      // For testing purposes only
      return reponse.status(400).send("Error deleted multiple accounts.\n");
    } else if (result.affectedRows == 0) {
      return response.status(400).send("Failed to delete account.\n");
    }
  });
}

// Helper function that changes the username of an account
function changeUsername(u, p, e, n, response) {
  var sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=? AND ??=?";
  var post = [db_table, username, n, username, u, password, p, email, e];
  connection.query(sql, post, function(err, result) {
    if (err) throw err;
    if (result.affectedRows == 1) {
      console.log("Account username changed.");
      return response.status(200).send("Successfully changed username.\n");
    } else if (result.affectedRows > 1) {
      // For testing purposes only
      return reponse.status(400).send("Error changed multiple account usernames.\n");
    } else if (result.affectedRows == 0) {
      return response.status(400).send("Failed to change username.\n");
    }
  });
}

// Helper function that changes the password of an account
function changePassword(u, p, e, n, response) {
  var sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=? AND ??=?";
  var post = [db_table, password, n, username, u, password, p, email, e];
  connection.query(sql, post, function(err, result) {
    if (err) throw err;
    if (result.affectedRows == 1) {
      console.log("Account password changed.");
      return response.status(200).send("Successfully changed password.\n");
    } else if (result.affectedRows > 1) {
      // For testing purposes only
      return reponse.status(400).send("Error changed multiple account passwords.\n");
    } else if (result.affectedRows == 0) {
      return response.status(400).send("Failed to change password.\n");
    }
  });
}

// Helper function that changes the email of an account
function changeEmail(u, p, e, n, response) {
  var sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=? AND ??=?";
  var post = [db_table, email, n, username, u, password, p, email, e];
  connection.query(sql, post, function(err, result) {
    if (err) throw err;
    if (result.affectedRows == 1) {
      console.log("Account email changed.");
      return response.status(200).send("Successfully changed email.\n");
    } else if (result.affectedRows > 1) {
      // For testing purposes only
      return reponse.status(400).send("Error changed multiple account emails.\n");
    } else if (result.affectedRows == 0) {
      return response.status(400).send("Failed to change email.\n");
    }
  });
}

// Helper function for forgotten password
function forgotPassword(u, e, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?";
  var post = [username, db_table, username, u, email, e];
  connection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Error invalid account.\n");
    } else {
      // SEND PASSWORD RESET EMAIL
      console.log("Password reset email sent.");
      return response.status(200).send("Password reset email sent.\n");
    }
  });
}

app.listen(port, (err) => {
  if (err) {
    return console.log('Listen error!', err);
  }
  console.log(`Server listening on port ${port}`);
});
