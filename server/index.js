var express = require('express');
var body_parser = require('body-parser');
var mysql = require('mysql');
var fs = require('fs');
var http = require('http');
var https = require('https');
var forceSSL = require('express-force-ssl');
var bcrypt = require('bcrypt');

var app = express();
var json_parser = body_parser.json();
var key = fs.readFileSync('ca/server.key');
var cert = fs.readFileSync('ca/server.crt');
var ca = fs.readFileSync('ca/ca.crt');
var sslCredentials = {
  key: key,
  cert: cert,
  ca: ca
};

// Constants used for http and https servers
const httpPort = 8080;
const httpsPort = 8443;

// Constants used for verifying JSON subsmission by users
const username = "username";
const password = "password";
const email = "email";
const newUsername = "newUsername";
const newPassword = "newPassword";
const newEmail = "newEmail";
const host = "localhost";

// Need to change username and password for production
const db_username = "wander";
const db_password = "wander";
const db_name = "wander";
const db_table = "accounts";
const loc_table = "locations";

// Constants used for password hashing
const saltRounds = 16;

// Create http and https servers and listen on specified ports
app.use(forceSSL);
var httpServer = http.createServer(app);
var httpsServer = https.createServer(sslCredentials, app);
httpServer.listen(httpPort, (err) => {
  if (err) {
    return console.log('HTTP server listen error!', err);
  }
  console.log(`HTTP server listening on port ${httpPort}`);
});
httpsServer.listen(httpsPort, (err) => {
  if (err) {
    return console.log('HTTPS server listen error!', err);
  }
  console.log(`HTTPS server listening on port ${httpsPort}`);
});

// Creates a connection to the MySQL database
var dbConnection = mysql.createConnection({
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

//Called when a POST request is made to /updateGPS
app.post('/updateGPS', json_parser, function(request, response){
if(!request.body) return response.sendStatus(500);

var post_variables = Object.keys(request.body);
if(Object.keys(request.body).length != 4 || post_variables[0] !== username || post_variables[1] !== latitude || post_variables[2] !== longitude || post_variables[3] !== time){
	return response.status(400).send("Invalid GPS update\n");
}
var u = request.body.username;
var lat = request.body.latitude;
var lon = request.body.longitude;
var date = request.body.time;

updateGPS(u, lat, lon, date);
}

// Helper function that registers a user if username and email does not already exist
function register(u, p, e, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? OR ??=?";
  var post = [username, db_table, username, u, email, e];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 0) {
      return response.status(400).send("Username or email already exists! Try again.\n");
    } else {
      bcrypt.hash(p, saltRounds, function(err, hash) {
        var sql = "INSERT INTO ?? SET ?";
        var post = {username: u, password: hash, email: e};
        dbConnection.query(sql, [db_table, post], function (err, result) {
          if (err) throw err;
          console.log("User account registered.");
          return response.send("Successfully registered an account.\n");
        });
      });
    }
  });
}

// Helper function that verifies user has an account and logs them in
function login(u, p, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=?"
  var post = [password, db_table, username, u];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or password. Try again.\n");
    } else {
      bcrypt.compare(p, result[0].password, function(err, res) {
        if (res !== true) {
          return response.status(400).send("Invalid username or password. Try again.\n");
        } else {
          console.log("User logged in.");
          return response.status(200).send("Successfully logged in.\n");
        }
      });
    }
  });
}

// Helper function that deletes an account
function deleteAccount(u, p, e, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?"
  var post = [password, db_table, username, u, email, e];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or email.\n");
    } else {
      bcrypt.compare(p, result[0].password, function(err, res) {
        if (res !== true) {
          return response.status(400).send("Invalid password. Try again.\n");
        } else {
          var sql = "DELETE FROM ?? WHERE ??=? AND ??=?";
          var post = [db_table, username, u, email, e];
          dbConnection.query(sql, post, function(err, result) {
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
      });
    }
  });
}

// Helper function that changes the username of an account
function changeUsername(u, p, e, n, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=?";
  var post = [username, db_table, username, n];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 0) {
      return response.status(400).send("Username already exists! Try again.\n");
    } else {
      var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?"
      var post = [password, db_table, username, u, email, e];
      dbConnection.query(sql, post, function (err, result) {
        if (err) throw err;
        if (Object.keys(result).length != 1) {
          return response.status(400).send("Invalid username or email.\n");
        } else {
          bcrypt.compare(p, result[0].password, function(err, res) {
            if (res !== true) {
              return response.status(400).send("Invalid password. Try again.\n");
            } else {
              var sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
              var post = [db_table, username, n, username, u, email, e];
              dbConnection.query(sql, post, function(err, result) {
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
          });
        }
      });
    }
  });
}

// Helper function that changes the password of an account
function changePassword(u, p, e, n, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?"
  var post = [password, db_table, username, u, email, e];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or email.\n");
    } else {
      bcrypt.compare(p, result[0].password, function(err, res) {
        if (res !== true) {
          return response.status(400).send("Invalid password. Try again.\n");
        } else {
          bcrypt.hash(n, saltRounds, function(err, hash) {
            var sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
            var post = [db_table, password, hash, username, u, email, e];
            dbConnection.query(sql, post, function(err, result) {
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
          });
        }
      });
    }
  });
}

// Helper function that changes the email of an account
function changeEmail(u, p, e, n, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=?";
  var post = [email, db_table, email, n];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 0) {
      return response.status(400).send("Email already exists! Try again.\n");
    } else {
      var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?"
      var post = [password, db_table, username, u, email, e];
      dbConnection.query(sql, post, function (err, result) {
        if (err) throw err;
        if (Object.keys(result).length != 1) {
          return response.status(400).send("Invalid username or email.\n");
        } else {
          bcrypt.compare(p, result[0].password, function(err, res) {
            if (res !== true) {
              return response.status(400).send("Invalid password. Try again.\n");
            } else {
              var sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
              var post = [db_table, email, n, username, u, email, e];
              dbConnection.query(sql, post, function(err, result) {
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
          });
        }
      });
    }
  });
}

// Helper function for forgotten password
function forgotPassword(u, e, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?";
  var post = [username, db_table, username, u, email, e];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or email.\n");
    } else {
      // SEND PASSWORD RESET EMAIL
      console.log("Password reset email sent.");
      return response.status(200).send("Password reset email sent.\n");
    }
  });
}

//Currently updateGPS only updates the usrname sql, it needs to push to the locations db too
// Helper function for GPS update
function updateGPS(u, lat, lon, date){
var sql = "UPDATE ?? SET ??=?, ??=?, ??=? WHERE ??=?";
//this will only work if there are only unique usernames in the system.
var post = [db_table, latitude, lat, longitude, lon, time, date, username, u];
dbConnection.query(sql, post, function(err, result){
if(err) throw err;
}); 
var sql = "INSERT INTO ?? SET ?";
var post= {username: u, latitude: lat, longitude: lon, time:date};
dbConnection.query(sql, [loc_table, post], function(err, result){
if (err) throw err;
});
}
