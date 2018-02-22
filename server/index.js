var express = require('express');
var bodyParser = require('body-parser');
var mysql = require('mysql');
var http = require('http');
var bcrypt = require('bcrypt');
var path = require('path');

var app = express();
var json_parser = bodyParser.json();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
  extended: true
}));

// Constants used for http server
const port = 3000;

// Constants used for verifying JSON subsmission by users
const username = "username";
const password = "password";
const email = "email";
const newUsername = "newUsername";
const newPassword = "newPassword";
const newEmail = "newEmail";
const host = "localhost";
const passwordResetToken = "passwordResetToken";
const passwordResetExpires = "passwordResetExpires";

// Need to change username and password for production
const db_username = "wander";
const db_password = "wander";
const db_name = "wander";
const db_table = "accounts";

// Constants used for password hashing
const saltRounds = 14;

// Create http server and listen on specified port
var httpServer = http.createServer(app);
httpServer.listen(port, (err) => {
  if (err) {
    return console.log('HTTP server listen error!', err);
  }
  console.log(`HTTP server listening on port ${port}`);
});

// Create connection to MySQL database
var dbConnection = mysql.createConnection({
  host: host,
  user: db_username,
  password: db_password,
  database: db_name
});

// Setup SendGrid for transactional email
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey(process.env.SENDGRID_API_KEY);

// Constants used for password reset
const crypto = require('crypto');

app.use(express.static(__dirname + '/website'));

// Called when a POST request is made to /registerAccount
app.post('/registerAccount', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 3 parameters (username, password, and email)
  if (Object.keys(request.body).length != 3) {
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
  if (Object.keys(request.body).length != 2) {
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
  if (Object.keys(request.body).length != 3) {
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
  if (Object.keys(request.body).length != 4) {
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
  if (Object.keys(request.body).length != 4) {
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
  if (Object.keys(request.body).length != 4) {
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
  if (Object.keys(request.body).length != 2) {
    return response.status(400).send("Invalid POST request\n");
  }
  var u = request.body.username;
  var e = request.body.email;

  forgotPassword(u, e, response);
});

// Called when a GET request is made to /resetPassword
app.get('/resetPassword', function(request, response) {
  if (Object.keys(request.query).length != 1 || !request.query.token) {
    return response.redirect('/');
  }
  response.sendFile(__dirname + '/website/resetPassword.html');
});

// Called when a POST request is made to /resetPassword
app.post('/resetPassword', function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request must have 2 parameters (newPassword and confirmPassword)
  if (Object.keys(request.body).length != 2) {
    return response.status(400).send("Invalid POST request\n");
  }
  if (Object.keys(request.query).length != 1 || !request.query.token) {
    return response.status(400).send("Invalid POST request\n");
  }
  var token = request.query.token;
  var newPassword = request.body.inputNewPassword;
  var confirmPassword = request.body.inputConfirmPassword;

  resetPassword(token, newPassword, confirmPassword, response);
});

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
          return response.status(200).send(JSON.stringify({"response":"Successfully registered an account."}));
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
          return response.status(200).send(JSON.stringify({"response":"Successfully logged in."}));
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
              return response.status(200).send(JSON.stringify({"response":"Successfully deleted account."}));
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
                  return response.status(200).send(JSON.stringify({"response":"Successfully changed username."}));
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
                return response.status(200).send(JSON.stringify({"response":"Successfully changed password."}));
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
                  return response.status(200).send(JSON.stringify({"response":"Successfully changed email."}));
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
  var sql = "SELECT * FROM ?? WHERE ??=? AND ??=?";
  var post = [db_table, username, u, email, e];
  dbConnection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or email.\n");
    } else {
      crypto.randomBytes(32, (err, buf) => {
        if (err) throw err;
        var token = buf.toString('hex');
        sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
        post = [db_table, passwordResetToken, token, username, u, email, e];
        dbConnection.query(sql, post, function(err, result) {
          if (err) throw err;
          if (result.affectedRows != 1) {
            return response.status(400).send("Invalid username or email.\n");
          } else {
            var expires = Date.now() + 3600000;
            sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
            post = [db_table, passwordResetExpires, expires, username, u, email, e];
            dbConnection.query(sql, post, function(err, result) {
              if (err) throw err;
              if (result.affectedRows != 1) {
                return response.status(400).send("Invalid username or email.\n");
              } else {
                const msg = {
                  to: e,
                  from: 'support@vvander.me',
                  subject: 'Wander Password Reset',
                  text: 'https://vvander.me/resetPassword?token=' + token,
                  html: '<strong>https://vvander.me/resetPassword?token=' + token + '</strong>',
                };
                sgMail.send(msg);
                console.log("Password reset email sent.");
                return response.status(200).send(JSON.stringify({"response":"Password reset email sent."}));
              }
            });
          }
        });
      });
    }
  });
}

// Helper function that resets an account password
function resetPassword(token, newPassword, confirmPassword, response) {
  if (newPassword != confirmPassword) {
    return response.status(400).send("Passwords did not match.\n");
  }
  var sql = "SELECT ??, ?? FROM ?? WHERE ??=?";
  var post = [email, passwordResetExpires, db_table, passwordResetToken, token];
  dbConnection.query(sql, post, function(err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(500).send("Password reset attempt has failed.\n");
    } else {
      if (Date.now() > result[0].passwordResetExpires) {
        return response.status(400).send("Password reset link has expired.\n");
      } else {
        var e = result[0].email;
        bcrypt.hash(newPassword, saltRounds, function(err, hash) {
          sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
          post = [db_table, password, hash, passwordResetToken, token, email, e];
          dbConnection.query(sql, post, function(err, result) {
            if (err) throw err;
            if (result.affectedRows != 1) {
              return response.status(500).send("Error resetting password.\n");
            } else {
              sql = "UPDATE ?? SET ??=NULL WHERE ??=? AND ??=?";
              post = [db_table, passwordResetExpires, passwordResetToken, token, email, e];
              dbConnection.query(sql, post, function(err, result) {
                if (err) throw err;
                if (result.affectedRows != 1) {
                  return response.status(500).send("Error resetting password.\n");
                } else {
                  sql = "UPDATE ?? SET ??=NULL WHERE ??=? AND ??=?";
                  post = [db_table, passwordResetToken, passwordResetToken, token, email, e];
                  dbConnection.query(sql, post, function(err, result) {
                    if (err) throw err;
                    if (result.affectedRows != 1) {
                      return response.status(500).send("Error resetting password.\n");
                    } else {
                      const msg = {
                        to: e,
                        from: 'support@vvander.me',
                        subject: 'Wander Password Reset Successful',
                        text: 'Your Wander password has been reset.',
                        html: '<strong>Your Wander password has been reset.</strong>',
                      };
                      sgMail.send(msg);
                      console.log("Account password reset.");
                      return response.redirect('/');
                    }
                  });
                }
              });
            }
          });
        });
      }
    }
  });
}
