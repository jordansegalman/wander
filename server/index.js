var express = require('express');
var bodyParser = require('body-parser');
var mysql = require('mysql');
var http = require('http');
var bcrypt = require('bcrypt');
var session = require('express-session');

var app = express();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
	extended: true
}));

// Constants used for http server
const port = 3000;

// Constants used for verifying JSON subsmission by users
const uid = "uid";
const username = "username";
const password = "password";
const email = "email";
const newUsername = "newUsername";
const newPassword = "newPassword";
const newEmail = "newEmail";
const host = "localhost";
const confirmed = "confirmed";
const passwordResetToken = "passwordResetToken";
const passwordResetExpires = "passwordResetExpires";
const latitude = "latitude";
const longitude = "longitude";
const time = "time";
const firstName = "firstname";
const lastName = "lastname";
const loc = "location";
const about = "about";

// Need to change username and password for production
const db_username = "wander";
const db_password = "wander";
const db_name = "wander";
const db_accounts = "accounts";
const db_profiles = "profiles";
const db_locations = "locations";

// Constant used for password hashing
const saltRounds = 10;

// Constant used for password reset and session
const crypto = require('crypto');

// Setup SendGrid for transactional email
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey(process.env.SENDGRID_API_KEY);

// Setup session
app.set('trust proxy', 1);
app.use(session({
	name: 'wander-cookie',
	secret: crypto.randomBytes(16).toString('hex'),
	resave: false,
	saveUninitialized: true,
	cookie: { domain: '.vvander.me', httpOnly: true, secure: true, maxAge: 31536000000 }
}));

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

// Serve 'website' directory
app.use(express.static('website'));

// Called when a POST request is made to /registerAccount
app.post('/registerAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 3 parameters (username, password, and email)
	if (Object.keys(request.body).length != 3 || !request.body.username || !request.body.password || !request.body.email) {
		return response.status(400).send("Invalid POST request\n");
	}

	var u = request.body.username;
	var p = request.body.password;
	var e = request.body.email;

	register(u, p, e, response);
});

// Called when a POST request is made to /login
app.post('/login', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (username and password)
	if (Object.keys(request.body).length != 2 || !request.body.username || !request.body.password) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session already authenticated
	if (request.session && request.session.authenticated && request.session.authenticated === true) {
		return response.status(400).send("User already logged in.\n");
	}

	var u = request.body.username;
	var p = request.body.password;

	login(u, p, request, response);
});

// Called when a POST request is made to /logout
app.post('/logout', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	logout(request, response);
});

// Called when a POST request is made to /verifySession
app.post('/verifySession', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send(JSON.stringify({"response":"fail"}));
	}

	return response.status(200).send(JSON.stringify({"response":"pass"}));
});

// Called when a POST request is made to /deleteAccount
app.post('/deleteAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (password)
	if (Object.keys(request.body).length != 1 || !request.body.password) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	var p = request.body.password;

	deleteAccount(p, request, response);
});

// Called when a GET request is made to /confirmEmail
app.get('/confirmEmail', function(request, response) {
	// GET request must have 1 query (email)
	if (Object.keys(request.query).length != 1 || !request.query.email) {
		return response.redirect('/');
	}

	var e = request.query.email;

	// Update account confirmed to true
	var sql = "UPDATE ?? SET ??=? WHERE ??=?";
	var post = [db_accounts, confirmed, true, email, e];
	dbConnection.query(sql, post, function (err, result){
		if (err) throw err;
		if (result.affectedRows == 1) {
			console.log("Account email confirmed.");
			return response.sendFile(__dirname + '/website/emailConfirmed.html');
		} else {
			return response.redirect('/');
		}
	});
});

// Called when a POST request is made to /changeUsername
app.post('/changeUsername', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (password and newUsername)
	if (Object.keys(request.body).length != 2 || !request.body.password || !request.body.newUsername) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	var p = request.body.password;
	var n = request.body.newUsername;

	changeUsername(p, n, request, response);
});

// Called when a POST request is made to /changePassword
app.post('/changePassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (password and newPassword)
	if (Object.keys(request.body).length != 2 || !request.body.password || !request.body.newPassword) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	var p = request.body.password;
	var n = request.body.newPassword;

	changePassword(p, n, request, response);
});

// Called when a POST request is made to /changeEmail
app.post('/changeEmail', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (password and newEmail)
	if (Object.keys(request.body).length != 2 || !request.body.password || !request.body.newEmail) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	var p = request.body.password;
	var n = request.body.newEmail;

	changeEmail(p, n, request, response);
});

// Called when a POST request is made to /forgotPassword
app.post('/forgotPassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (username and email)
	if (Object.keys(request.body).length != 2 || !request.body.username || !request.body.email) {
		return response.status(400).send("Invalid POST request\n");
	}

	var u = request.body.username;
	var e = request.body.email;

	forgotPassword(u, e, response);
});

// Called when a GET request is made to /resetPassword
app.get('/resetPassword', function(request, response) {
	// GET request must have 1 query (token)
	if (Object.keys(request.query).length != 1 || !request.query.token) {
		return response.redirect('/');
	}

	response.sendFile(__dirname + '/website/resetPassword.html');
});

// Called when a POST request is made to /resetPassword
app.post('/resetPassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (newPassword and confirmPassword)
	if (Object.keys(request.body).length != 2 || !request.body.inputNewPassword || !request.body.inputConfirmPassword) {
		return response.status(400).send("Invalid POST request\n");
	}

	// POST request must have 1 query (token)
	if (Object.keys(request.query).length != 1 || !request.query.token) {
		return response.status(400).send("Invalid POST request\n");
	}

	var token = request.query.token;
	var newPassword = request.body.inputNewPassword;
	var confirmPassword = request.body.inputConfirmPassword;

	resetPassword(token, newPassword, confirmPassword, response);
});

// Called when a GET request is made to /linkedInProfile
app.get('/linkedInProfile', function(request, response) {
	response.sendFile(__dirname + '/website/linkedInProfile.html');
});

// Called when a POST request is made to /updateLinkedIn
app.post('/updateLinkedIn', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 5 parameters (firstname, lastname, email, loc, and about)
	if (Object.keys(request.body).length != 5 || !request.body.firstname || !request.body.lastname || !request.body.email || !request.body.loc || !request.body.about) {
		return response.status(400).send("Invalid POST request\n");
	}

	var f = request.body.firstname;
	var l = request.body.lastname;
	var e = request.body.email;
	var lo = request.body.loc;
	var a = request.body.about;

	updateLinkedInProfile(f, l, e, lo, a, response);
});

// Called when a POST request is made to /getProfile
app.post('/getProfile', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	getProfile(request, response);
});

// Called when a POST request is made to /updateLocation
app.post('/updateLocation', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (latitude and longitude)
	if (Object.keys(request.body).length != 2 || !request.body.latitude || !request.body.longitude) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	var lat = request.body.latitude;
	var lon = request.body.longitude;

	updateLocation(lat, lon, request, response);
});

// Helper function that registers a user if username and email does not already exist
function register(u, p, e, response) {
	// Check if username or email already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=? OR ??=?";
	var post = [username, db_accounts, username, u, email, e];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 0) {
			return response.status(400).send("Username or email already exists! Try again.\n");
		} else {
			// Hash password and insert username, hash, and email
			bcrypt.hash(p, saltRounds, function(err, hash) {
				var userID = crypto.randomBytes(8).toString('hex');
				// Check if user ID already exists
				var sql = "SELECT ?? FROM ?? WHERE ??=?";
				var post = [uid, db_accounts, uid, userID];
				dbConnection.query(sql, post, function (err, result) {
					if (err) throw err;
					if (Object.keys(result).length != 0) {
						return response.status(500).send("User ID collision!\n");
					} else {
						var sql = "INSERT INTO ?? SET ?";
						var post = {uid: userID, username: u, password: hash, email: e};
						dbConnection.query(sql, [db_accounts, post], function (err, result) {
							if (err) throw err;
							// Send registration confirm email
							const msg = {
								to: e,
								from: 'support@vvander.me',
								subject: 'Welcome to Wander!',
								text: 'Hey ' + u + '! You have registered for a Wander account. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + e,
								html: '<strong>Hey ' + u + '! You have registered for a Wander account. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + e + '</strong>',
							};
							sgMail.send(msg);
							console.log("Account registered.");
							return response.status(200).send(JSON.stringify({"response":"pass"}));
						});
					}
				});
			});
		}
	});
}

// Helper function that verifies user has an account and logs them in
function login(u, p, request, response) {
	// Get password hash and email for username
	var sql = "SELECT ??,??,?? FROM ?? WHERE ??=?";
	var post = [uid, password, email, db_accounts, username, u];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 1) {
			return response.status(400).send("Invalid username or password. Try again.\n");
		} else {
			// Compare sent password hash to account password hash
			bcrypt.compare(p, result[0].password, function(err, res) {
				if (res === true) {
					request.session.authenticated = true;
					request.session.uid = result[0].uid;
					request.session.username = u;
					request.session.email = result[0].email;
					console.log("User logged in.");
					return response.status(200).send(JSON.stringify({"response":"pass", "email":request.session.email}));
				} else {
					return response.status(400).send("Invalid username or password. Try again.\n");
				}
			});
		}
	});
}

// Helper function that verifies user is logged in and can now log out
function logout(request, response) {
	delete request.session.authenticated;
	delete request.session.uid;
	delete request.session.username;
	delete request.session.email;
	console.log("User logged out.");
	return response.status(200).send(JSON.stringify({"response":"pass"}));
}

// Helper function that deletes an account
function deleteAccount(p, request, response) {
	// Get password hash for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [password, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 1) {
			return response.status(500).send("User ID not found.\n");
		} else {
			// Compare sent password hash to account password hash
			bcrypt.compare(p, result[0].password, function(err, res) {
				if (res !== true) {
					return response.status(400).send("Invalid password. Try again.\n");
				} else {
					// Delete account for user ID
					var sql = "DELETE FROM ?? WHERE ??=?";
					var post = [db_accounts, uid, request.session.uid];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.affectedRows == 1) {
							// Delete location data for user ID
							var sql = "DELETE FROM ?? WHERE ??=?";
							var post = [db_locations, uid, request.session.uid];
							dbConnection.query(sql, post, function(err, result){
								if (err) throw err;
								// Send account deletion notification email
								const msg = {
									to: request.session.email,
									from: 'support@vvander.me',
									subject: 'Wander Account Deleted',
									text: 'Hey ' + request.session.username + '! You have successfully deleted your Wander account. We are sorry to see you go.',
									html: '<strong>Hey ' + request.session.username + '! You have successfully deleted your Wander account. We are sorry to see you go.</strong>',
								};
								sgMail.send(msg);
								delete request.session.authenticated;
								delete request.session.uid;
								delete request.session.username;
								delete request.session.email;
								console.log("Account deleted.");
								return response.status(200).send(JSON.stringify({"response":"pass"}));
							});
						} else if (result.affectedRows > 1) {
							// For testing purposes only
							return reponse.status(500).send("Error deleted multiple accounts.\n");
						} else if (result.affectedRows == 0) {
							return response.status(500).send("Failed to delete account.\n");
						}
					});
				}
			});
		}
	});
}

// Helper function that changes the username of an account
function changeUsername(p, n, request, response) {
	// Check if new username already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [username, db_accounts, username, n];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 0) {
			return response.status(400).send("Username already exists! Try again.\n");
		} else {
			// Get password hash for user ID
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [password, db_accounts, uid, request.session.uid];
			dbConnection.query(sql, post, function (err, result) {
				if (err) throw err;
				if (Object.keys(result).length != 1) {
					return response.status(500).send("User ID not found.\n");
				} else {
					// Compare sent password hash to account password hash
					bcrypt.compare(p, result[0].password, function(err, res) {
						if (res !== true) {
							return response.status(400).send("Invalid password. Try again.\n");
						} else {
							// Update username for user ID
							var sql = "UPDATE ?? SET ??=? WHERE ??=?";
							var post = [db_accounts, username, n, uid, request.session.uid];
							dbConnection.query(sql, post, function(err, result) {
								if (err) throw err;
								if (result.affectedRows == 1) {
									// Send username change notification email
									const msg = {
										to: request.session.email,
										from: 'support@vvander.me',
										subject: 'Wander Username Changed',
										text: 'You have changed your Wander account username from ' + request.session.username + ' to ' + n + '.',
										html: '<strong>You have changed your Wander account username from ' + request.session.username + ' to ' + n + '.</strong>',
									};
									sgMail.send(msg);
									request.session.username = n;
									console.log("Account username changed.");
									return response.status(200).send(JSON.stringify({"response":"pass"}));
								} else if (result.affectedRows > 1) {
									// For testing purposes only
									return reponse.status(500).send("Error changed multiple account usernames.\n");
								} else if (result.affectedRows == 0) {
									return response.status(500).send("Failed to change username.\n");
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
function changePassword(p, n, request, response) {
	// Get password hash for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [password, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 1) {
			console.log("err 1");
			return response.status(500).send("User ID not found.\n");
		} else {
			// Compare sent password hash to account password hash
			bcrypt.compare(p, result[0].password, function(err, res) {
				if (res !== true) {
					console.log("err 2");
					return response.status(400).send("Invalid password. Try again.\n");
				} else {
					// Hash new password and update password for user ID
					bcrypt.hash(n, saltRounds, function(err, hash) {
						var sql = "UPDATE ?? SET ??=? WHERE ??=?";
						var post = [db_accounts, password, hash, uid, request.session.uid];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							if (result.affectedRows == 1) {
								// Send password change notification email
								const msg = {
									to: request.session.email,
									from: 'support@vvander.me',
									subject: 'Wander Password Changed',
									text: 'You have changed your Wander account password.',
									html: '<strong>You have changed your Wander account password.</strong>',
								};
								sgMail.send(msg);
								console.log("Account password changed.");
								return response.status(200).send(JSON.stringify({"response":"pass"}));
							} else if (result.affectedRows > 1) {
								// For testing purposes only
								return reponse.status(500).send("Error changed multiple account passwords.\n");
							} else if (result.affectedRows == 0) {
								console.log("err 3");
								return response.status(500).send("Failed to change password.\n");
							}
						});
					});
				}
			});
		}
	});
}

// Helper function that changes the email of an account
function changeEmail(p, n, request, response) {
	// Check if new email already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [email, db_accounts, email, n];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 0) {
			return response.status(400).send("Email already exists! Try again.\n");
		} else {
			// Get password hash for user ID
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [password, db_accounts, uid, request.session.uid];
			dbConnection.query(sql, post, function (err, result) {
				if (err) throw err;
				if (Object.keys(result).length != 1) {
					return response.status(500).send("User ID not found.\n");
				} else {
					bcrypt.compare(p, result[0].password, function(err, res) {
						if (res !== true) {
							return response.status(400).send("Invalid password. Try again.\n");
						} else {
							// Update email for user ID
							var sql = "UPDATE ?? SET ??=? WHERE ??=?";
							var post = [db_accounts, email, n, uid, request.session.uid];
							dbConnection.query(sql, post, function(err, result) {
								if (err) throw err;
								if (result.affectedRows == 1) {
									// Set confirmed to false for user ID
									var sql = "UPDATE ?? SET ??=? WHERE ??=?";
									var post = [db_accounts, confirmed, false, uid, request.session.uid];
									dbConnection.query(sql, post, function (err, result){
										if (err) throw err;
										if (result.affectedRows == 1) {
											// Send email change notification email to old email
											const oldmsg = {
												to: request.session.email,
												from: 'support@vvander.me',
												subject: 'Wander Email Changed',
												text: 'You have changed your Wander account email to ' + n + '.',
												html: '<strong>You have changed your Wander account email to ' + n + '.</strong>',
											};
											sgMail.send(oldmsg);
											request.session.email = n;
											// Send email confirm email to new email
											const newmsg = {
												to: request.session.email,
												from: 'support@vvander.me',
												subject: 'Confirm Your Email',
												text: 'Hey ' + request.session.username + '! You have changed your Wander account email. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + request.session.email,
												html: '<strong>Hey ' + request.session.username + '! You have changed your Wander account email. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + request.session.email + '</strong>',
											};
											sgMail.send(newmsg);
											console.log("Account email changed.");
											return response.status(200).send(JSON.stringify({"response":"pass"}));
										} else {
											return response.status(500).send("Error changing email.\n");
										}
									});
								} else if (result.affectedRows > 1) {
									// For testing purposes only
									return reponse.status(500).send("Error changed multiple account emails.\n");
								} else if (result.affectedRows == 0) {
									return response.status(500).send("Failed to change email.\n");
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
	// Check that account exists for username and email
	var sql = "SELECT * FROM ?? WHERE ??=? AND ??=?";
	var post = [db_accounts, username, u, email, e];
	dbConnection.query(sql, post, function (err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 1) {
			console.log(result);
			return response.status(400).send("Invalid username or email.\n");
		} else {
			// Generate password reset token for username and email
			crypto.randomBytes(32, (err, buf) => {
				if (err) throw err;
				var token = buf.toString('hex');
				sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
				post = [db_accounts, passwordResetToken, token, username, u, email, e];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					if (result.affectedRows != 1) {
						return response.status(400).send("Invalid username or email.\n");
					} else {
						// Update password reset expire time for username and email
						var expires = Date.now() + 3600000;
						sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
						post = [db_accounts, passwordResetExpires, expires, username, u, email, e];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							if (result.affectedRows != 1) {
								return response.status(400).send("Invalid username or email.\n");
							} else {
								// Send password reset request email
								const msg = {
									to: e,
									from: 'support@vvander.me',
									subject: 'Wander Password Reset',
									text: 'Hey ' + u + '! You have requested a password reset for your Wander account. Click the following link to reset your password: https://vvander.me/resetPassword?token=' + token,
									html: '<strong>Hey ' + u + '! You have requested a password reset for your Wander account. Click the following link to reset your password: https://vvander.me/resetPassword?token=' + token + '</strong>',
								};
								sgMail.send(msg);
								console.log("Password reset email sent.");
								return response.status(200).send(JSON.stringify({"response":"pass"}));
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
	// Check that newPassword and confirmPassword are the same
	if (newPassword != confirmPassword) {
		return response.status(400).send("Passwords did not match.\n");
	}
	// Get email and passwordResetExpires for passwordResetToken
	var sql = "SELECT ??, ?? FROM ?? WHERE ??=?";
	var post = [email, passwordResetExpires, db_accounts, passwordResetToken, token];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (Object.keys(result).length != 1) {
			return response.status(400).send("Invalid password reset token.\n");
		} else {
			// Check if password reset token is expired
			if (Date.now() > result[0].passwordResetExpires) {
				return response.status(400).send("Password reset link has expired.\n");
			} else {
				// Hash new password and update password for passwordResetToken and email
				var e = result[0].email;
				bcrypt.hash(newPassword, saltRounds, function(err, hash) {
					sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
					post = [db_accounts, password, hash, passwordResetToken, token, email, e];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.affectedRows != 1) {
							return response.status(500).send("Error resetting password.\n");
						} else {
							// Set passwordResetExpires to null for passwordResetToken and email
							sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
							post = [db_accounts, passwordResetExpires, null, passwordResetToken, token, email, e];
							dbConnection.query(sql, post, function(err, result) {
								if (err) throw err;
								if (result.affectedRows != 1) {
									return response.status(500).send("Error resetting password.\n");
								} else {
									// Set passwordResetToken to null for passwordResetToken and email
									sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
									post = [db_accounts, passwordResetToken, null, passwordResetToken, token, email, e];
									dbConnection.query(sql, post, function(err, result) {
										if (err) throw err;
										if (result.affectedRows != 1) {
											return response.status(500).send("Error resetting password.\n");
										} else {
											// Send password reset notification email
											const msg = {
												to: e,
												from: 'support@vvander.me',
												subject: 'Wander Password Reset Successful',
												text: 'Your Wander account password has been reset.',
												html: '<strong>Your Wander account password has been reset.</strong>',
											};
											sgMail.send(msg);
											console.log("Account password reset.");
											return response.sendFile(__dirname + '/website/passwordReset.html');
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

// Helper function for updating LinkedIn profile information
function updateLinkedInProfile(f, l, e, lo, a, response) {
	// Get profile for email
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_profiles, email, e];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (Object.keys(result).length == 0) {
			// Create profile if none exists for email
			var sql = "INSERT INTO ?? SET ??=?, ??=?, ??=?, ??=?, ??=?";
			var post = [db_profiles, firstName, f, lastName, l, email, e, loc, lo, about, a];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				console.log("LinkedIn profile created."); 
				return response.status(200).send(JSON.stringify({"response":"pass"}));
			});
		} else {
			// Update profile if already exists for email
			var sql = "UPDATE ?? SET ??=?, ??=?, ??=?, ??=? WHERE ??=?";
			var post = [db_profiles, firstName, f, lastName, l, email, e, loc, lo, about, a];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				console.log("LinkedIn profile updated."); 
				return response.status(200).send(JSON.stringify({"response":"pass"}));
			});
		}
	});
}

// Helper function for getting LinkedIn profile information
function getProfile(request, response) {
	// Get profile for email and respond with profile data
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_profiles, email, request.session.email];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (Object.keys(result).length == 0) {
			return response.status(400).send("No profile.\n");
		} else {
			var f = result[0].firstname;
			var l = result[0].lastname;
			var e = result[0].email;
			var lo = result[0].location;
			var a = result[0].about;
			return response.status(200).send(JSON.stringify({"response":"pass", firstname:f, lastname:l, email:e, loc:lo, about:a}));
		}
	});
}

// Helper function for updating user location data
function updateLocation(lat, lon, request, response) {
	// Insert uid, longitude, latitude, and time
	var sql = "INSERT INTO ?? SET ??=?, ??=?, ??=?, ??=?";
	var currentTime = Date.now();
	var weekOld = currentTime - 604800000;
	var post = [db_locations, uid, request.session.uid, longitude, lon, latitude, lat, time, currentTime];
	dbConnection.query(sql, post, function(err, result){
		if (err) throw err;
		// Delete all location data more than a week old
		var sql = "DELETE FROM ?? WHERE ?? BETWEEN 0 AND ?";
		var post = [db_locations, time, weekOld];
		dbConnection.query(sql, post, function(err, result){
			if (err) throw err;
		});
		return response.status(200).send(JSON.stringify({"response":"success"}));	
	});
}

// Serve 404 error page
app.use(function(req, res, next) {
	res.status(404).sendFile(__dirname + '/website/404.html');
});

// Server 500 error page
app.use(function(err, req, res, next) {
	console.error(err.stack);
	res.status(500).sendFile(__dirname + '/website/500.html');
});
