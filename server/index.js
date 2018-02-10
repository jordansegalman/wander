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
const host = "localhost";

// For actual implementation, we need to set username & password differently
const db_username = "root";
const db_password = "root";
const db_name = "wander";
const db_table = "accounts";

// Creates a connection to the MySQL database
var connection = mysql.createConnection({
  multipleStatements: true,
  host: host,
  user: db_username,
  password: db_password,
  database: db_name
});

app.get('/', function(request, response) {
  response.send("GET request");
});

// Called when a POST request is mdoe to /registerAccount
app.post('/registerAccount', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request for login must have 2 parameters (username and password)
  if (Object.keys(request.body).length != 3 || post_variables[0] !== username || post_variables[1] !== password || post_variables[2] !== email) {
    return response.status(400).send("Invalid POST request");
  }
  var u = request.body.username;
  var p = request.body.password;
  var e = request.body.email;

  checkDatabase(u, p, e, response);
});

// Called when a POST request is mdoe to /login
app.post('/login', json_parser, function(request, response) {
  // If the object request.body is null, respond with status 500 'Internal Server Error'
  if (!request.body) return response.sendStatus(500);

  var post_variables = Object.keys(request.body);
  // POST request for login must have 2 parameters (username and password)
  if (Object.keys(request.body).length != 2 || post_variables[0] !== username || post_variables[1] !== password) {
    return response.status(400).send("Invalid POST request");
  }
  var u = request.body.username;
  var p = request.body.password;

  login(u, p, response);
});

// Helper function that checks if username exists in database
function checkDatabase(u, p, e, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=?";
  var post = [username, db_table, username, u];
  connection.query(sql, post, function (err, result) {
    if (err) throw err;

    if (Object.keys(result).length != 0) {
      return response.status(400).send("Username already exists! Try again.");
    } else {
      register(u,p,e, response);
    }
  });
}

// Helper function that registers a user if username does not already exist
function register(u, p, e, response) {
  var sql = "INSERT INTO ?? SET ?";
  var post = {username: u, password: p, email: e};
  connection.query(sql, [db_table, post], function (err, result) {
    if (err) throw err;
    console.log("User account registered.");
    response.send("Successfully registered an account!");
  });
}

// Helper function that verifies user has an account and logs them in
function login(u, p, response) {
  var sql = "SELECT ?? FROM ?? WHERE ??=? AND ??=?";
  var post = [username, db_table, username, u, password, p];
  connection.query(sql, post, function (err, result) {
    if (err) throw err;
    if (Object.keys(result).length != 1) {
      return response.status(400).send("Invalid username or password. Try again.");
    } else {
      return response.status(200).send("Logged In");
    }
  });
}

app.listen(port, (err) => {
  if (err) {
    return console.log('Error!', err);
  }
  console.log(`Server listening on port ${port}`);
});
