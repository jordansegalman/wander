var express = require('express');
var bodyParser = require('body-parser');
var mysql = require('mysql');
var http = require('http');
var bcrypt = require('bcrypt');
var session = require('express-session');
var fs = require('fs');
var graphlib = require('graphlib');
var schedule = require('node-schedule');
var validator = require('validator');
var admin = require('firebase-admin');
var geocluster = require('geocluster');

// Load environment variables
require('dotenv').config();

// Constants used for http server
const url = process.env.URL;
const port = process.env.PORT;

// Constants used for JSON
const uid = "uid";
const username = "username";
const password = "password";
const email = "email";
const newUsername = "newUsername";
const newPassword = "newPassword";
const newEmail = "newEmail";
const confirmed = "confirmed";
const passwordResetToken = "passwordResetToken";
const passwordResetExpires = "passwordResetExpires";
const crossRadius = "crossRadius";
const latitude = "latitude";
const longitude = "longitude";
const time = "time";
const name = "name";
const about = "about";
const interests = "interests";
const picture = "picture";
const registrationToken = "registrationToken";
const googleID = "googleID";
const facebookID = "facebookID";
const timesCrossed = "timesCrossed";
const approved = "approved";
const message = "message";
const uidFrom = "uidFrom";
const uidTo = "uidTo";
const title = "title";
const description = "description";
const reason = "reason";
const banned = "banned";
const populationMultiplier = "populationMultiplier";
const matchLimit = "matchLimit";

// Constants used for MySQL
const db_host = process.env.DB_HOST;
const db_username = process.env.DB_USERNAME;
const db_password = process.env.DB_PASSWORD;
const db_name = process.env.DB_NAME;
const db_accounts = "accounts";
const db_profiles = "profiles";
const db_locations = "locations";
const db_firebase = "firebase";
const db_messages = "messages";
const db_tags = "tags";
const db_offenses = "offenses";

// Constant used for password hashing
const saltRounds = 10;

// Constants used for matching
const MATCH_THRESHOLD = 10;				// 10 crossed paths
const UNMATCHED_MATCH_THRESHOLD = 3 * MATCH_THRESHOLD;
const CROSS_TIME = 30000;				// 30 seconds
const CROSS_COOLDOWN = 1800000;				// 30 minutes
const MATCHES_NOTIFY_CRON = '0 20 * * * *';		// Every day at 20:00
const NO_MATCHES_NOTIFY_CRON = '0 20 * * * *';		// Every day at 20:00
const WARN_THRESHOLD = 3;				// Warn after 3 offenses
const BAN_THRESHOLD = 5;				// Ban after 5 offenses
const POPULAR_LOCATIONS_CRON = '0 0 * * * *';		// Every day at 0:00
const NEARBY_USERS_RADIUS = 5280;			// 1 mile
const POPULATION_MULTIPLIER_LOW_CUTOFF = 10;		// 10 users
const POPULATION_MULTIPLIER_HIGH_CUTOFF = 500;		// 500 users
const POPULATION_MULTIPLIER_LOW = 1.25;
const POPULATION_MULTIPLIER_HIGH = 0.75;

// Constant used for password reset and session
const crypto = require('crypto');

// Setup Firebase
var serviceAccount = require(process.env.FIREBASE_CREDENTIALS_JSON);
admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
	databaseURL: process.env.FIREBASE_DATABASE_URL
});

// Setup SendGrid for transactional email
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey(process.env.SENDGRID_API_KEY);
const supportEmail = process.env.SUPPORT_EMAIL;

// Setup express
var app = express();
app.use(bodyParser.json({
	limit: '50mb'
}));
app.use(bodyParser.urlencoded({
	extended: true
}));

// Setup session
app.set('trust proxy', 1);
app.use(session({
	name: process.env.COOKIE_NAME,
	secret: process.env.COOKIE_SECRET,
	resave: false,
	saveUninitialized: false,
	cookie: { domain: process.env.COOKIE_DOMAIN, httpOnly: true, secure: true, maxAge: 31536000000 }
}));

// Create connection to MySQL database
var dbConnection = mysql.createConnection({
	host: db_host,
	user: db_username,
	password: db_password,
	database: db_name,
	charset: 'utf8mb4_unicode_520_ci'
});

// Setup server
var httpServer;
var matchGraph;
var popularLocations;
startServer();

// Starts the server
function startServer() {
	if (fs.existsSync('matchGraph.json')) {
		// Read existing match graph
		matchGraph = graphlib.json.read(JSON.parse(fs.readFileSync('matchGraph.json')));
		console.log('Match graph read.');
		// Update popular locations
		updatePopularLocations();
		// Delete all Firebase registration tokens
		var sql = "TRUNCATE TABLE ??";
		var post = [db_firebase];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			console.log('Firebase registration tokens deleted.');
			// Create HTTP server
			httpServer = http.createServer(app);
			// Setup Socket.IO
			setupSocketIO();
			// Setup scheduled tasks
			setupSchedules();
			// Start HTTP server
			httpServer.listen(port, (err) => {
				if (err) {
					return console.log('Server listen error!', err);
				}
				console.log('Server listening on port ' + port + '.');
			});
		});
	} else {
		// Create new match graph and insert all user IDs as nodes
		matchGraph = new graphlib.Graph({ directed: true, multigraph: true, compound: false });
		var sql = "SELECT ?? FROM ??";
		var post = [uid, db_accounts];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			for (var i = 0; i < result.length; i++) {
				matchGraph.setNode(result[i].uid);
			}
			writeMatchGraph();
			console.log('Match graph created.');
			// Update popular locations
			updatePopularLocations();
			// Delete all Firebase registration tokens
			var sql = "TRUNCATE TABLE ??";
			var post = [db_firebase];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				console.log('Firebase registration tokens deleted.');
				// Create HTTP server
				httpServer = http.createServer(app);
				// Setup Socket.IO
				setupSocketIO();
				// Setup scheduled tasks
				setupSchedules();
				// Start HTTP server
				httpServer.listen(port, (err) => {
					if (err) {
						return console.log('Server listen error!', err);
					}
					console.log('Server listening on port ' + port + '.');
				});
			});
		});
	}
}

// Setup Socket.IO
function setupSocketIO() {
	var io = require('socket.io')(httpServer);
	// Holds array of sockets for every user ID
	var connectedSockets = {};
	io.on('connection', (socket) => {
		socket.on('disconnect', () => {
		});
		socket.on('initialize', (incomingData) => {
			// Add socket to connectedSockets
			var parsedData = JSON.parse(incomingData);
			if (validateUid(parsedData['uid'])) {
				if (connectedSockets.hasOwnProperty(parsedData['uid'])) {
					var index = connectedSockets[parsedData['uid']].indexOf(socket);
					if (index == -1) {
						connectedSockets[parsedData['uid']].push(socket);
					}
				} else {
					connectedSockets[parsedData['uid']] = [];
					connectedSockets[parsedData['uid']].push(socket);
				}
			}
		});
		socket.on('terminate', (incomingData) => {
			// Remove socket from connectedSockets
			var parsedData = JSON.parse(incomingData);
			if (validateUid(parsedData['uid'])) {
				if (connectedSockets.hasOwnProperty(parsedData['uid'])) {
					var index = connectedSockets[parsedData['uid']].indexOf(socket);
					if (index > -1) {
						connectedSockets[parsedData['uid']].splice(index, 1);
					}
					if (connectedSockets[parsedData['uid']].length == 0) {
						delete connectedSockets[parsedData['uid']];
					}
				}
			}
		});
		socket.on('message', (incomingData) => {
			var parsedData = JSON.parse(incomingData);
			if (validateUid(parsedData['from']) && validateUid(parsedData['to']) && validateMessage(parsedData['message'])) {
				// Add message to messages table
				var sql = "INSERT INTO ?? SET ??=?, ??=?, ??=?, ??=?";
				var post = [db_messages, uidFrom, parsedData['from'], uidTo, parsedData['to'], message, parsedData['message'], time, parsedData['time']];
				dbConnection.query(sql, post, function(err, result){
					if (err) throw err;
					// Get name of user message sent from
					var sql = "SELECT ?? FROM ?? WHERE ??=?";
					var post = [name, db_profiles, uid, parsedData['from']];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (connectedSockets.hasOwnProperty(parsedData['to'])) {
							// If message sent to user who is connected, send over socket
							for (var i = 0; i < connectedSockets[parsedData['to']].length; i++) {
								var outgoingData = {};
								outgoingData['from'] = parsedData['from'];
								outgoingData['message'] = parsedData['message'];
								outgoingData['time'] = parsedData['time'];
								outgoingData['name'] = result[0].name;
								connectedSockets[parsedData['to']][i].emit('message', JSON.stringify(outgoingData));
							}
						} else {
							// If message sent to user who is not connected, send notification
							var sql = "SELECT ?? FROM ?? WHERE ??=?";
							var post = [registrationToken, db_firebase, uid, parsedData['to']];
							dbConnection.query(sql, post, function(err, res) {
								if (err) throw err;
								if (res.length > 0) {
									for (var i = 0; i < res.length; i++) {
										var message = {
											data: {
												type: 'Chat Message',
												title: result[0].name,
												body: parsedData['message'],
												uid: parsedData['from']
											},
											token: res[i].registrationToken,
											android: {
												ttl: 3600000,
												priority: 'high',
											}
										};
										admin.messaging().send(message)
											.then((response) => {
												console.log('Successfully sent message notification.');
											})
											.catch((error) => {
												console.log(error);
											});
									}
								}
							});
						}
					});
				});
			}
		});
	});
}

// Setup scheduled tasks
function setupSchedules() {
	schedule.scheduleJob(MATCHES_NOTIFY_CRON, function() {
		notifyMatches();
	});
	schedule.scheduleJob(NO_MATCHES_NOTIFY_CRON, function() {
		notifyNoMatches();
	});
	schedule.scheduleJob(POPULAR_LOCATIONS_CRON, function() {
		updatePopularLocations();
	});
}

// Serve 'website' directory
app.use(express.static('website'));

// Called when a POST request is made to /registerAccount
app.post('/registerAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 3 parameters (username, password, and email)
	if (Object.keys(request.body).length != 3 || !request.body.username || !request.body.password || !request.body.email) {
		return response.status(400).send("Invalid POST request");
	}

	// Validate username, password, and email
	if (validateUsername(request.body.username)) {
		var u = request.body.username;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid username"}));
	}
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
	}
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid email"}));
	}

	register(u, p, e, response);
});

// Called when a POST request is made to /login
app.post('/login', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (username and password)
	if (Object.keys(request.body).length != 2 || !request.body.username || !request.body.password) {
		return response.status(400).send("Invalid POST request");
	}

	// If session already authenticated
	if (request.session && ((request.session.authenticated && request.session.authenticated === true) || (request.session.googleAuthenticated && request.session.googleAuthenticated === true) || (request.session.facebookAuthenticated && request.session.facebookAuthenticated === true))) {
		return response.status(400).send("User already logged in.");
	}

	// Validate username and password
	if (validateUsername(request.body.username)) {
		var u = request.body.username;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid username or password"}));
	}
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid username or password"}));
	}

	login(u, p, request, response);
});

// Called when a POST request is made to /googleLogin
app.post('/googleLogin', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (googleID and email)
	if (Object.keys(request.body).length != 2 || !request.body.googleID || !request.body.email) {
		return response.status(400).send("Invalid POST request");
	}

	// If session already authenticated
	if (request.session && ((request.session.authenticated && request.session.authenticated === true) || (request.session.googleAuthenticated && request.session.googleAuthenticated === true) || (request.session.facebookAuthenticated && request.session.facebookAuthenticated === true))) {
		return response.status(400).send("User already logged in.");
	}

	var id = request.body.googleID;
	// Validate email
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid email"}));
	}

	googleLogin(id, e, request, response);
});

// Called when a POST request is made to /facebookLogin
app.post('/facebookLogin', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (facebookID and email)
	if (Object.keys(request.body).length != 2 || !request.body.facebookID || !request.body.email) {
		return response.status(400).send("Invalid POST request");
	}

	// If session already authenticated
	if (request.session && ((request.session.authenticated && request.session.authenticated === true) || (request.session.googleAuthenticated && request.session.googleAuthenticated === true) || (request.session.facebookAuthenticated && request.session.facebookAuthenticated === true))) {
		return response.status(400).send("User already logged in.");
	}

	var id = request.body.facebookID;
	// Validate email
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid email"}));
	}

	facebookLogin(id, e, request, response);
});

// Called when a POST request is made to /logout
app.post('/logout', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	logout(request, response);
});

// Called when a POST request is made to /verifySession
app.post('/verifySession', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send(JSON.stringify({"response":"fail"}));
	}

	// Check if account banned
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [banned, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result[0].banned == true) {
			return response.status(400).send("Account banned.");
		}
	});

	if (request.session && request.session.googleAuthenticated && request.session.googleAuthenticated === true) {
		return response.status(200).send(JSON.stringify({"response":"google","uid":request.session.uid}));
	} else if (request.session && request.session.facebookAuthenticated && request.session.facebookAuthenticated === true) {
		return response.status(200).send(JSON.stringify({"response":"facebook","uid":request.session.uid}));
	} else {
		return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
	}
});

// Called when a POST request is made to /deleteAccount
app.post('/deleteAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (password)
	if (Object.keys(request.body).length != 1 || !request.body.password) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.");
	}

	// Validate password
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
	}

	deleteAccount(p, request, response);
});

// Called when a POST request is made to /googleDeleteAccount
app.post('/googleDeleteAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || !request.session.googleAuthenticated || request.session.googleAuthenticated === false) {
		return response.status(400).send("User not logged in.");
	}

	googleDeleteAccount(request, response);
});

// Called when a POST request is made to /facebookDeleteAccount
app.post('/facebookDeleteAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || !request.session.facebookAuthenticated || request.session.facebookAuthenticated === false) {
		return response.status(400).send("User not logged in.");
	}

	facebookDeleteAccount(request, response);
});

// Called when a GET request is made to /confirmEmail
app.get('/confirmEmail', function(request, response) {
	// GET request must have 1 query (email)
	if (Object.keys(request.query).length != 1 || !request.query.email) {
		return response.redirect('/');
	}

	// Validate email
	if (validateEmail(request.query.email)) {
		var e = normalizeEmail(request.query.email);
	} else {
		return response.status(400).send("Invalid email.");
	}

	// Update account confirmed to true
	var sql = "UPDATE ?? SET ??=? WHERE ??=?";
	var post = [db_accounts, confirmed, true, email, e];
	dbConnection.query(sql, post, function(err, result){
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
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.");
	}

	// Validate password and newUsername
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
	}
	if (validateUsername(request.body.newUsername)) {
		var n = request.body.newUsername;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid new username"}));
	}

	changeUsername(p, n, request, response);
});

// Called when a POST request is made to /changePassword
app.post('/changePassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (password and newPassword)
	if (Object.keys(request.body).length != 2 || !request.body.password || !request.body.newPassword) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.");
	}

	// Validate password and newPassword
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
	}
	if (validatePassword(request.body.newPassword)) {
		var n = request.body.newPassword;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid new password"}));
	}

	changePassword(p, n, request, response);
});

// Called when a POST request is made to /changeEmail
app.post('/changeEmail', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (password and newEmail)
	if (Object.keys(request.body).length != 2 || !request.body.password || !request.body.newEmail) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.");
	}

	// Validate password and newEmail
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
	}
	if (validateEmail(request.body.newEmail)) {
		var n = normalizeEmail(request.body.newEmail);
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid new email"}));
	}

	changeEmail(p, n, request, response);
});

// Called when a POST request is made to /changeCrossRadius
app.post('/changeCrossRadius', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (newCrossRadius)
	if (Object.keys(request.body).length != 1 || !request.body.newCrossRadius) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate newCrossRadius
	if (validateCrossRadius(request.body.newCrossRadius)) {
		var n = request.body.newCrossRadius;
	} else {
		return response.status(400).send("Cross radius must be an integer with a minimum of 10 and a maximum of 5280.");
	}

	changeCrossRadius(n, request, response);
});

// Called when a POST request is made to /getCrossRadius
app.post('/getCrossRadius', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getCrossRadius(request, response);
});

// Called when a POST request is made to /changeMatchLimit
app.post('/changeMatchLimit', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (newMatchLimit)
	if (Object.keys(request.body).length != 1 || !request.body.newMatchLimit) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate newMatchLimit
	if (validateMatchLimit(request.body.newMatchLimit)) {
		var n = request.body.newMatchLimit;
	} else {
		return response.status(400).send("Match limit must be an integer with a minimum of 0 and a maximum of 25.");
	}

	changeMatchLimit(n, request, response);
});

//Called when a POST request is made to /getMatchLimit
app.post('/getMatchLimit', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getMatchLimit(request, response);
});

// Called when a POST request is made to /forgotPassword
app.post('/forgotPassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (username and email)
	if (Object.keys(request.body).length != 2 || !request.body.username || !request.body.email) {
		return response.status(400).send("Invalid POST request");
	}

	// Validate username and email
	if (validateUsername(request.body.username)) {
		var u = request.body.username;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid username"}));
	}
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid email"}));
	}

	forgotPassword(u, e, response);
});

// Called when a GET request is made to /resetPassword
app.get('/resetPassword', function(request, response) {
	// GET request must have 1 query (token)
	if (Object.keys(request.query).length != 1 || !request.query.token || !validatePasswordResetToken(request.query.token)) {
		return response.redirect('/');
	}

	response.sendFile(__dirname + '/website/resetPassword.html');
});

// Called when a POST request is made to /resetPassword
app.post('/resetPassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (inputNewPassword and inputConfirmPassword)
	if (Object.keys(request.body).length != 2 || !request.body.inputNewPassword || !request.body.inputConfirmPassword) {
		return response.status(400).send("Invalid POST request");
	}

	// POST request must have 1 query (token)
	if (Object.keys(request.query).length != 1 || !request.query.token) {
		return response.status(400).send("Invalid POST request");
	}

	// Validate token, inputNewPassword, and inputConfirmPassword
	if (validatePasswordResetToken(request.query.token)) {
		var token = request.query.token;
	} else {
		return response.status(400).send("Invalid password reset token.");
	}
	if (validatePassword(request.body.inputNewPassword) && validatePassword(request.body.inputConfirmPassword)) {
		var newPassword = request.body.inputNewPassword;
		var confirmPassword = request.body.inputConfirmPassword;
	} else {
		return response.status(400).send("New password must only contain ASCII characters and must have a minimum length of 8 characters and a maximum length of 64 characters.");
	}

	// Check that newPassword and confirmPassword are the same
	if (newPassword != confirmPassword) {
		return response.status(400).send("Passwords did not match.");
	}

	resetPassword(token, newPassword, confirmPassword, response);
});

// Called when a POST request is made to /addFirebaseRegistrationToken
app.post('/addFirebaseRegistrationToken', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (firebaseRegistrationToken)
	if (Object.keys(request.body).length != 1 || !request.body.firebaseRegistrationToken) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	var token = request.body.firebaseRegistrationToken;

	addFirebaseRegistrationToken(token, request, response);
});

// Called when a GET request is made to /linkedInProfile
app.get('/linkedInProfile', function(request, response) {
	response.sendFile(__dirname + '/website/linkedInProfile.html');
});

// Called when a POST request is made to /updateLinkedIn
app.post('/updateLinkedIn', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 3 parameters (email, name, and about)
	if (Object.keys(request.body).length != 3 || !request.body.email || !request.body.name || !request.body.about) {
		return response.status(400).send("Invalid POST request");
	}

	// Validate email, name, and about
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send("Email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.");
	}
	if (validateName(request.body.name)) {
		var n = request.body.name;
	} else {
		return response.status(400).send("Name must only contain ASCII characters and have a maximum length of 32 characters.");
	}
	if (validateAbout(request.body.about)) {
		var a = request.body.about;
	} else {
		return response.status(400).send("About has a maximum length of 255 characters.");
	}

	updateLinkedInProfile(e, n, a, response);
});

// Called when a POST request is made to /updateProfile
app.post('/updateProfile', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 4 parameters (name, about, interests, and picture)
	if (Object.keys(request.body).length != 4 || !request.body.name || !request.body.about || !request.body.interests || !request.body.picture) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate name, about, interests, and picture
	if (validateName(request.body.name)) {
		var n = request.body.name;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid name"}));
	}
	if (validateAbout(request.body.about)) {
		var a = request.body.about;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid about"}));
	}
	if (validateInterests(request.body.interests)) {
		var i = request.body.interests;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid interests"}));
	}
	if (validatePicture(request.body.picture)) {
		var p = request.body.picture;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid picture"}));
	}

	updateProfile(n, a, i, p, request, response);
});

// Called when a POST request is made to /getProfile
app.post('/getProfile', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getProfile(request, response);
});

// Called when a POST request is made to /updateLocation
app.post('/updateLocation', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (latitude and longitude)
	if (Object.keys(request.body).length != 2 || !request.body.latitude || !request.body.longitude) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate coordinates
	if (validateCoordinates(request.body.latitude, request.body.longitude)) {
		var lat = request.body.latitude;
		var lon = request.body.longitude;
	} else {
		return response.status(400).send("Invalid coordinates.");
	}

	updateLocation(lat, lon, request, response);
});

// Called when a POST request is made to /approveUser
app.post('/approveUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	approveUser(u, request, response);
});

// Called when a POST request is made to /unapproveUser
app.post('/unapproveUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	unapproveUser(u, request, response);
});

// Called when a POST request is made to /unmatchUser
app.post('/unmatchUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	unmatchUser(u, request, response);
});

// Called when a POST request is made to /blockUser
app.post('/blockUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	blockUser(u, request, response);
});

// Called when a POST request is made to /unblockUser
app.post('/unblockUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	unblockUser(u, request, response);
});

// Called when a POST request is made to /reportUser
app.post('/reportUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (uid and reason)
	if (Object.keys(request.body).length != 2 || !request.body.uid || !request.body.reason) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid and reason
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}
	if (validateReportReason(request.body.reason)) {
		var r = request.body.reason;
	} else {
		return response.status(400).send(JSON.stringify({"response":"Invalid reason"}));
	}

	reportUser(u, r, request, response);
});

// Called when a POST request is made to /getLocationsForHeatmap
app.post('/getLocationsForHeatmap', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.send(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not autheticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getLocationsForHeatmap(request, response);
});

// Called when a POST request is made to /getAllLocationsForHeatmap
app.post('/getAllLocationsForHeatmap', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.send(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not autheticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getAllLocationsForHeatmap(request, response);
});

// Called when a POST request is made to /getAllMatches
app.post('/getAllMatches', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getAllMatches(request, response);
});

// Called when a POST request is made to /getMatch
app.post('/getMatch', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	getMatch(u, request, response);
});

// Called when a POST request is made to /getCrossLocations
app.post('/getCrossLocations', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	getCrossLocations(u, request, response);
});

// Called when a POST request is made to /getMessages
app.post('/getMessages', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	getMessages(u, request, response);
});

// Called when a POST request is made to /getAllBlocked
app.post('/getAllBlocked', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getAllBlocked(request, response);
});

// Called when a POST request is made to /getBlocked
app.post('/getBlocked', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	getBlocked(u, request, response);
});

// Called when a POST request is made to /storeTagData
app.post('/storeTagData', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (tag)
	if (Object.keys(request.body).length != 1 || !request.body.tag) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	var t = request.body.tag;

	storeTagData(t, request, response);
});

// Called when a POST request is made to /getTagData
app.post('/getTagData', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	getTagData(request, response);
});

// Called when POST request is made to /getMatchTagData
app.post('/getMatchTagData', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.");
	}

	getMatchTagData(u, request, response);
});

// Called when POST request is made to /deleteTagData
app.post('/deleteTagData', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (tag)
	if (Object.keys(request.body).length != 1 || !request.body.tag) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.");
	}

	var t = request.body.tag;

	deleteTagData(t, request, response);
});

// Called when a POST request is made to /getStatistics
app.post('/getStatistics', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (latitude and longitude)
	if (Object.keys(request.body).length != 2 || !request.body.latitude || !request.body.longitude) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate coordinates
	if (validateCoordinates(request.body.latitude, request.body.longitude)) {
		var lat = request.body.latitude;
		var lon = request.body.longitude;
	} else {
		return response.status(400).send("Invalid coordinates.\n");
	}

	getStatistics(lat, lon, request, response);
});

// Called when a POST request is made to /getLocationSuggestions
app.post('/getLocationSuggestions', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	getLocationSuggestions(request, response);
});

// Called when a POST request is made to /notifyInterestsChange
app.post('/notifyInterestsChange', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	notifyInterestsChange(request, response);
});

// Validates a user ID
function validateUid(uid) {
	return !validator.isEmpty(uid) && validator.isHexadecimal(uid) && validator.isLength(uid, {min: 16, max: 16});
}

// Validates a username
function validateUsername(username) {
	return !validator.isEmpty(username) && validator.isAlphanumeric(username) && validator.isLength(username, {min: 4, max: 24});
}

// Validates a password
function validatePassword(password) {
	return !validator.isEmpty(password) && validator.isAscii(password) && validator.isLength(password, {min: 8, max: 64});
}

// Validates an email
function validateEmail(email) {
	return !validator.isEmpty(email) && validator.isEmail(email) && validator.isLength(email, {min: 3, max: 255});
}

// Normalizes an email
function normalizeEmail(email) {
	return validator.normalizeEmail(email);
}

// Validates a cross radius
function validateCrossRadius(crossRadius) {
	return !validator.isEmpty(crossRadius) && validator.isInt(crossRadius, {min: 10, max: 5280});
}

// Validates a password reset token
function validatePasswordResetToken(passwordResetToken) {
	return !validator.isEmpty(passwordResetToken) && validator.isHexadecimal(passwordResetToken) && validator.isLength(passwordResetToken, {min: 64, max: 64});
}

// Validates latitude and longitude coordinates
function validateCoordinates(lat, lon) {
	return !validator.isEmpty(lat) && !validator.isEmpty(lon) && validator.isLatLong(lat + ',' + lon);
}

// Validates match limit
function validateMatchLimit(matchLimit){
	return !validator.isEmpty(matchLimit) && validator.isInt(matchLimit, {min: 0, max: 25});
}

// Validates a profile name
function validateName(name) {
	return !validator.isEmpty(name) && validator.isAscii(name) && validator.isLength(name, {min: 1, max: 32});
}

// Validates profile about
function validateAbout(about) {
	return !validator.isEmpty(about) && validator.isByteLength(about, {min: 1, max: 255});
}

// Validates profile interests
function validateInterests(interests) {
	return !validator.isEmpty(interests) && validator.isAscii(interests) && validator.isLength(interests, {min: 1, max: 255});
}

// Validates a profile picture
function validatePicture(picture) {
	return !validator.isEmpty(picture) && validator.isByteLength(interests, {min: 1, max: 16777215});
}

// Validates a chat message
function validateMessage(message) {
	return !validator.isEmpty(message) && validator.isLength(message, {min: 1, max: 1024});
}

// Validates a tag title
function validateTagTitle(tagTitle) {
	return !validator.isEmpty(tagTitle) && validator.isByteLength(tagTitle, {min: 1, max: 255});
}

// Validates a tag description
function validateTagDescription(tagDescription) {
	return !validator.isEmpty(tagDescription) && validator.isByteLength(tagDescription, {min: 1, max: 512});
}

// Validates a report reason
function validateReportReason(reportReason) {
	return !validator.isEmpty(reportReason) && validator.isByteLength(reportReason, {min: 1, max: 1024});
}

// Registers a user if username and email does not already exist
function register(u, p, e, response) {
	// Check if username already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [username, db_accounts, username, u];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 0) {
			return response.status(400).send(JSON.stringify({"response":"Username taken"}));
		} else {
			// Check if email already exists
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [username, db_accounts, email, e];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 0) {
					return response.status(400).send(JSON.stringify({"response":"Email taken"}));
				} else {
					// Hash password and insert username, hash, and email
					bcrypt.hash(p, saltRounds, function(err, hash) {
						var userID = crypto.randomBytes(8).toString('hex');
						// Check if user ID already exists
						var sql = "SELECT ?? FROM ?? WHERE ??=?";
						var post = [uid, db_accounts, uid, userID];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							if (result.length != 0) {
								return response.status(500).send("User ID collision!");
							} else {
								// Create account in accounts table
								var sql = "INSERT INTO ?? SET ?";
								var post = {uid: userID, username: u, password: hash, email: e};
								dbConnection.query(sql, [db_accounts, post], function(err, result) {
									if (err) throw err;
									// Create profile in profiles table
									var sql = "INSERT INTO ?? SET ??=?, ??=?";
									var post = [db_profiles, uid, userID, email, e];
									dbConnection.query(sql, post, function(err, result){
										if (err) throw err;
										setDefaultProfilePicture(userID);
										// Send registration confirm email
										const msg = {
											to: e,
											from: supportEmail,
											subject: 'Welcome to Wander!',
											text: 'Hey ' + u + '! You have registered for a Wander account. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + e,
											html: '<strong>Hey ' + u + '! You have registered for a Wander account. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + e + '</strong>',
										};
										sgMail.send(msg);
										matchGraph.setNode(userID);
										writeMatchGraph();
										console.log("Account registered.");
										return response.status(200).send(JSON.stringify({"response":"pass"}));
									});
								});
							}
						});
					});
				}
			});
		}
	});
}

// Sets the default profile picture for a profile
function setDefaultProfilePicture(u) {
	var defaultProfilePicture = fs.readFileSync('default_profile.png', {encoding: 'base64'});
	var sql = "UPDATE ?? SET ??=? WHERE ??=?";
	var post = [db_profiles, picture, defaultProfilePicture, uid, u];
	dbConnection.query(sql, post, function(err, result){
		if (err) throw err;
	});
}

// Verifies user has an account and logs them in
function login(u, p, request, response) {
	// Get user ID, password hash, email, and banned status for username
	var sql = "SELECT ??,??,??,?? FROM ?? WHERE ??=?";
	var post = [uid, password, email, banned, db_accounts, username, u];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(400).send(JSON.stringify({"response":"Invalid username or password"}));
		} else {
			if (result[0].banned == true) {
				return response.status(400).send(JSON.stringify({"response":"Account banned"}));
			}
			// Compare sent password hash to account password hash
			bcrypt.compare(p, result[0].password, function(err, res) {
				if (res === true) {
					request.session.authenticated = true;
					request.session.uid = result[0].uid;
					request.session.username = u;
					request.session.email = result[0].email;
					console.log("User logged in.");
					return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
				} else {
					return response.status(400).send(JSON.stringify({"response":"Invalid username or password"}));
				}
			});
		}
	});
}

// Logs a user in with Google
function googleLogin(id, e, request, response) {
	// Check if account exists for Google account
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_accounts, googleID, id];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length == 0) {
			// Check if email already exists
			var sql = "SELECT * FROM ?? WHERE ??=?";
			var post = [db_accounts, email, e];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 0) {
					return response.status(400).send(JSON.stringify({"response":"Email taken"}));
				} else {
					var userID = crypto.randomBytes(8).toString('hex');
					// Check if user ID already exists
					var sql = "SELECT ?? FROM ?? WHERE ??=?";
					var post = [uid, db_accounts, uid, userID];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.length != 0) {
							return response.status(500).send("User ID collision!");
						} else {
							// Create account in accounts table
							var sql = "INSERT INTO ?? SET ?";
							var post = {uid: userID, email: e, googleID: id};
							dbConnection.query(sql, [db_accounts, post], function(err, result) {
								if (err) throw err;
								// Create profile in profiles table
								var sql = "INSERT INTO ?? SET ??=?, ??=?";
								var post = [db_profiles, uid, userID, email, e];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
									setDefaultProfilePicture(userID);
									// Send registration confirm email
									const msg = {
										to: e,
										from: supportEmail,
										subject: 'Welcome to Wander!',
										text: 'You have registered for a Wander account with your Google account. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + e,
										html: '<strong>You have registered for a Wander account with your Google account. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + e + '</strong>',
									};
									sgMail.send(msg);
									matchGraph.setNode(userID);
									writeMatchGraph();
									request.session.googleAuthenticated = true;
									request.session.uid = userID;
									request.session.email = e;
									console.log("Account created and logged in with Google.");
									return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
								});
							});
						}
					});
				}
			});
		} else if (result.length == 1) {
			if (result[0].banned == true) {
				return response.status(400).send(JSON.stringify({"response":"Account banned"}));
			}
			if (result[0].email == e) {
				request.session.googleAuthenticated = true;
				request.session.uid = result[0].uid;
				request.session.email = result[0].email;
				console.log("User logged in with Google.");
				return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
			} else {
				request.session.googleAuthenticated = true;
				request.session.uid = result[0].uid;
				request.session.email = e;
				// Update account email for user ID
				var sql = "UPDATE ?? SET ??=? WHERE ??=?";
				var post = [db_accounts, email, e, uid, request.session.uid];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					// Update profile email for user ID
					var sql = "UPDATE ?? SET ??=? WHERE ??=?";
					var post = [db_profiles, email, e, uid, request.session.uid];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						console.log("User logged in with Google.");
						return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
					});
				});
			}
		} else {
			return response.status(500).send("Error with Google login.");
		}
	});
}

// Logs a user in with Facebook
function facebookLogin(id, e, request, response) {
	// Check if account exists for Facebook account
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_accounts, facebookID, id];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length == 0) {
			// Check if email already exists
			var sql = "SELECT * FROM ?? WHERE ??=?";
			var post = [db_accounts, email, e];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 0) {
					return response.status(400).send(JSON.stringify({"response":"Email taken"}));
				} else {
					var userID = crypto.randomBytes(8).toString('hex');
					// Check if user ID already exists
					var sql = "SELECT ?? FROM ?? WHERE ??=?";
					var post = [uid, db_accounts, uid, userID];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.length != 0) {
							return response.status(500).send("User ID collision!");
						} else {
							// Create account in accounts table
							var sql = "INSERT INTO ?? SET ?";
							var post = {uid: userID, email: e, facebookID: id};
							dbConnection.query(sql, [db_accounts, post], function(err, result) {
								if (err) throw err;
								// Create profile in profiles table
								var sql = "INSERT INTO ?? SET ??=?, ??=?";
								var post = [db_profiles, uid, userID, email, e];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
									setDefaultProfilePicture(userID);
									// Send registration confirm email
									const msg = {
										to: e,
										from: supportEmail,
										subject: 'Welcome to Wander!',
										text: 'You have registered for a Wander account with your Facebook account. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + e,
										html: '<strong>You have registered for a Wander account with your Facebook account. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + e + '</strong>',
									};
									sgMail.send(msg);
									matchGraph.setNode(userID);
									writeMatchGraph();
									request.session.facebookAuthenticated = true;
									request.session.uid = userID;
									request.session.email = e;
									console.log("Account created and logged in with Facebook.");
									return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
								});
							});
						}
					});
				}
			});
		} else if (result.length == 1) {
			if (result[0].banned == true) {
				return response.status(400).send(JSON.stringify({"response":"Account banned"}));
			}
			if (result[0].email == e) {
				request.session.facebookAuthenticated = true;
				request.session.uid = result[0].uid;
				request.session.email = result[0].email;
				console.log("User logged in with Facebook.");
				return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
			} else {
				request.session.facebookAuthenticated = true;
				request.session.uid = result[0].uid;
				request.session.email = e;
				// Update account email for user ID
				var sql = "UPDATE ?? SET ??=? WHERE ??=?";
				var post = [db_accounts, email, e, uid, request.session.uid];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					// Update profile email for user ID
					var sql = "UPDATE ?? SET ??=? WHERE ??=?";
					var post = [db_profiles, email, e, uid, request.session.uid];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						console.log("User logged in with Facebook.");
						return response.status(200).send(JSON.stringify({"response":"pass","uid":request.session.uid}));
					});
				});
			}
		} else {
			return response.status(500).send("Error with Facebook login.");
		}
	});
}

// Verifies user is logged in and logs them out
function logout(request, response) {
	if (request.session.firebaseRegistrationToken) {
		// Delete current Firebase registration token
		var sql = "DELETE FROM ?? WHERE ??=?";
		var post = [db_firebase, registrationToken, request.session.firebaseRegistrationToken];
		dbConnection.query(sql, post, function(err, result){
			if (err) throw err;
		});
	}
	// Destroy the session
	request.session.destroy(function(err) {
		console.log("User logged out.");
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	});
}

// Deletes an account
function deleteAccount(p, request, response) {
	// Get password hash for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [password, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(500).send("User ID not found.");
		} else {
			// Compare sent password hash to account password hash
			bcrypt.compare(p, result[0].password, function(err, res) {
				if (res !== true) {
					return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
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
								// Delete profile for user ID
								var sql = "DELETE FROM ?? WHERE ??=?";
								var post = [db_profiles, uid, request.session.uid];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
									if (request.session.firebaseRegistrationToken) {
										// Delete current Firebase registration token
										var sql = "DELETE FROM ?? WHERE ??=?";
										var post = [db_firebase, registrationToken, request.session.firebaseRegistrationToken];
										dbConnection.query(sql, post, function(err, result){
											if (err) throw err;
										});
									}
									// Send account deletion notification email
									const msg = {
										to: request.session.email,
										from: supportEmail,
										subject: 'Wander Account Deleted',
										text: 'Hey ' + request.session.username + '! You have successfully deleted your Wander account. We are sorry to see you go.',
										html: '<strong>Hey ' + request.session.username + '! You have successfully deleted your Wander account. We are sorry to see you go.</strong>',
									};
									sgMail.send(msg);
									matchGraph.removeNode(request.session.uid);
									writeMatchGraph();
									// Destroy the session
									request.session.destroy(function(err) {
										console.log("Account deleted.");
										return response.status(200).send(JSON.stringify({"response":"pass"}));
									});
								});
							});
						} else if (result.affectedRows > 1) {
							// For testing purposes only
							return response.status(500).send("Error deleted multiple accounts.");
						} else if (result.affectedRows == 0) {
							return response.status(500).send("Failed to delete account.");
						}
					});
				}
			});
		}
	});
}

// Deletes an account created with Google
function googleDeleteAccount(request, response) {
	// Check that account exists
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(500).send("User ID not found.");
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
						// Delete profile for user ID
						var sql = "DELETE FROM ?? WHERE ??=?";
						var post = [db_profiles, uid, request.session.uid];
						dbConnection.query(sql, post, function(err, result){
							if (err) throw err;
							if (request.session.firebaseRegistrationToken) {
								// Delete current Firebase registration token
								var sql = "DELETE FROM ?? WHERE ??=?";
								var post = [db_firebase, registrationToken, request.session.firebaseRegistrationToken];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
								});
							}
							// Send account deletion notification email
							const msg = {
								to: request.session.email,
								from: supportEmail,
								subject: 'Wander Account Deleted',
								text: 'You have successfully deleted your Wander account which was created with your Google account. We are sorry to see you go.',
								html: '<strong>You have successfully deleted your Wander account which was created with your Google account. We are sorry to see you go.</strong>',
							};
							sgMail.send(msg);
							matchGraph.removeNode(request.session.uid);
							writeMatchGraph();
							// Destroy the session
							request.session.destroy(function(err) {
								console.log("Wander account created with Google account deleted.");
								return response.status(200).send(JSON.stringify({"response":"pass"}));
							});
						});
					});
				} else if (result.affectedRows > 1) {
					// For testing purposes only
					return response.status(500).send("Error deleted multiple accounts.");
				} else if (result.affectedRows == 0) {
					return response.status(500).send("Failed to delete account.");
				}
			});
		}
	});
}

// Deletes an account created with Facebook
function facebookDeleteAccount(request, response) {
	// Check that account exists
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(500).send("User ID not found.");
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
						// Delete profile for user ID
						var sql = "DELETE FROM ?? WHERE ??=?";
						var post = [db_profiles, uid, request.session.uid];
						dbConnection.query(sql, post, function(err, result){
							if (err) throw err;
							if (request.session.firebaseRegistrationToken) {
								// Delete current Firebase registration token
								var sql = "DELETE FROM ?? WHERE ??=?";
								var post = [db_firebase, registrationToken, request.session.firebaseRegistrationToken];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
								});
							}
							// Send account deletion notification email
							const msg = {
								to: request.session.email,
								from: supportEmail,
								subject: 'Wander Account Deleted',
								text: 'You have successfully deleted your Wander account which was created with your Facebook account. We are sorry to see you go.',
								html: '<strong>You have successfully deleted your Wander account which was created with your Facebook account. We are sorry to see you go.</strong>',
							};
							sgMail.send(msg);
							matchGraph.removeNode(request.session.uid);
							writeMatchGraph();
							// Destroy the session
							request.session.destroy(function(err) {
								console.log("Wander account created with Facebook account deleted.");
								return response.status(200).send(JSON.stringify({"response":"pass"}));
							});
						});
					});
				} else if (result.affectedRows > 1) {
					// For testing purposes only
					return response.status(500).send("Error deleted multiple accounts.");
				} else if (result.affectedRows == 0) {
					return response.status(500).send("Failed to delete account.");
				}
			});
		}
	});
}

// Changes the username of an account
function changeUsername(p, n, request, response) {
	// Check if new username already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [username, db_accounts, username, n];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 0) {
			return response.status(400).send(JSON.stringify({"response":"Username taken"}));
		} else {
			// Get password hash for user ID
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [password, db_accounts, uid, request.session.uid];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 1) {
					return response.status(500).send("User ID not found.");
				} else {
					// Compare sent password hash to account password hash
					bcrypt.compare(p, result[0].password, function(err, res) {
						if (res !== true) {
							return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
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
										from: supportEmail,
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
									return response.status(500).send("Error changed multiple account usernames.");
								} else if (result.affectedRows == 0) {
									return response.status(500).send("Failed to change username.");
								}
							});
						}
					});
				}
			});
		}
	});
}

// Changes the password of an account
function changePassword(p, n, request, response) {
	// Get password hash for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [password, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(500).send("User ID not found.");
		} else {
			// Compare sent password hash to account password hash
			bcrypt.compare(p, result[0].password, function(err, res) {
				if (res !== true) {
					return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
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
									from: supportEmail,
									subject: 'Wander Password Changed',
									text: 'You have changed your Wander account password.',
									html: '<strong>You have changed your Wander account password.</strong>',
								};
								sgMail.send(msg);
								console.log("Account password changed.");
								return response.status(200).send(JSON.stringify({"response":"pass"}));
							} else if (result.affectedRows > 1) {
								// For testing purposes only
								return response.status(500).send("Error changed multiple account passwords.");
							} else if (result.affectedRows == 0) {
								return response.status(500).send("Failed to change password.");
							}
						});
					});
				}
			});
		}
	});
}

// Changes the email of an account
function changeEmail(p, n, request, response) {
	// Check if new email already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [email, db_accounts, email, n];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 0) {
			return response.status(400).send(JSON.stringify({"response":"Email taken"}));
		} else {
			// Get password hash for user ID
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [password, db_accounts, uid, request.session.uid];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 1) {
					return response.status(500).send("User ID not found.");
				} else {
					// Compare sent password hash to account password hash
					bcrypt.compare(p, result[0].password, function(err, res) {
						if (res !== true) {
							return response.status(400).send(JSON.stringify({"response":"Invalid password"}));
						} else {
							// Update account email for user ID
							var sql = "UPDATE ?? SET ??=? WHERE ??=?";
							var post = [db_accounts, email, n, uid, request.session.uid];
							dbConnection.query(sql, post, function(err, result) {
								if (err) throw err;
								if (result.affectedRows == 1) {
									// Set confirmed to false for user ID
									var sql = "UPDATE ?? SET ??=? WHERE ??=?";
									var post = [db_accounts, confirmed, false, uid, request.session.uid];
									dbConnection.query(sql, post, function(err, result){
										if (err) throw err;
										if (result.affectedRows == 1) {
											// Update profile email for user ID
											var sql = "UPDATE ?? SET ??=? WHERE ??=?";
											var post = [db_profiles, email, n, uid, request.session.uid];
											dbConnection.query(sql, post, function(err, result) {
												if (err) throw err;
												// Send email change notification email to old email
												const oldmsg = {
													to: request.session.email,
													from: supportEmail,
													subject: 'Wander Email Changed',
													text: 'You have changed your Wander account email to ' + n + '.',
													html: '<strong>You have changed your Wander account email to ' + n + '.</strong>',
												};
												sgMail.send(oldmsg);
												request.session.email = n;
												// Send email confirm email to new email
												const newmsg = {
													to: request.session.email,
													from: supportEmail,
													subject: 'Confirm Your Email',
													text: 'Hey ' + request.session.username + '! You have changed your Wander account email. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + request.session.email,
													html: '<strong>Hey ' + request.session.username + '! You have changed your Wander account email. Click the following link to confirm your email: ' + url + '/confirmEmail?email=' + request.session.email + '</strong>',
												};
												sgMail.send(newmsg);
												console.log("Account email changed.");
												return response.status(200).send(JSON.stringify({"response":"pass"}));
											});
										} else {
											return response.status(500).send("Error changing email.");
										}
									});
								} else if (result.affectedRows > 1) {
									// For testing purposes only
									return response.status(500).send("Error changed multiple account emails.");
								} else if (result.affectedRows == 0) {
									return response.status(500).send("Failed to change email.");
								}
							});
						}
					});
				}
			});
		}
	});
}

// Changes the crossRadius of an account
function changeCrossRadius(n, request, response) {
	// Update cross radius for user ID
	var sql = "UPDATE ?? SET ??=? WHERE ??=?";
	var post = [db_accounts, crossRadius, n, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.affectedRows == 1) {
			console.log("Account cross radius changed.");
			return response.status(200).send(JSON.stringify({"response":"pass"}));
		} else if (result.affectedRows > 1) {
			// For testing purposes only
			return response.status(500).send("Error changed multiple account cross radii.");
		} else if (result.affectedRows == 0) {
			return response.status(500).send("Failed to change cross radius.");
		}
	});
}

// Gets the crossRadius of an account
function getCrossRadius(request, response) {
	// Get cross radius for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [crossRadius, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length == 0) {
			return response.status(500).send("Error getting cross radius.");
		} else {
			return response.status(200).send(JSON.stringify({"response":"pass", crossRadius:result[0].crossRadius}));
		}
	});
}

// Changes the matchLimit of an account
function changeMatchLimit(n, request, response) {
	// Update match limit for user ID
	var sql = "UPDATE ?? SET ??=? WHERE ??=?";
	var post = [db_accounts, matchLimit, n, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.affectedRows == 1) {
			console.log("Account match limit changed.");
			return response.status(200).send(JSON.stringify({"response":"pass"}));
		} else if (result.affectedRows > 1) {
			// For testing purposes only
			return response.status(500).send("Error changed multiple account match limits.");
		} else if (result.affectedRows == 0) {
			return response.status(500).send("Failed to change match limit.");
		}
	});
}

// Gets the matchLimit of an account
function getMatchLimit(request, response) {
	// Get match limit for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [matchLimit, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length == 0) {
			return response.status(500).send("Error getting match limit.");
		} else {
			return response.status(200).send(JSON.stringify({"response":"pass", matchLimit:result[0].matchLimit}));
		}
	});
}

// Sends password reset email
function forgotPassword(u, e, response) {
	// Check that account exists for username and email
	var sql = "SELECT * FROM ?? WHERE ??=? AND ??=?";
	var post = [db_accounts, username, u, email, e];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(400).send(JSON.stringify({"response":"No account found"}));
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
						return response.status(400).send(JSON.stringify({"response":"No account found"}));
					} else {
						// Update password reset expire time for username and email
						var expires = Date.now() + 3600000;
						sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
						post = [db_accounts, passwordResetExpires, expires, username, u, email, e];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							if (result.affectedRows != 1) {
								return response.status(400).send(JSON.stringify({"response":"No account found"}));
							} else {
								// Send password reset request email
								const msg = {
									to: e,
									from: supportEmail,
									subject: 'Wander Password Reset',
									text: 'Hey ' + u + '! You have requested a password reset for your Wander account. Click the following link to reset your password: ' + url + '/resetPassword?token=' + token,
									html: '<strong>Hey ' + u + '! You have requested a password reset for your Wander account. Click the following link to reset your password: ' + url + '/resetPassword?token=' + token + '</strong>',
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

// Resets an account password
function resetPassword(token, newPassword, confirmPassword, response) {
	// Get email and passwordResetExpires for passwordResetToken
	var sql = "SELECT ??, ?? FROM ?? WHERE ??=?";
	var post = [email, passwordResetExpires, db_accounts, passwordResetToken, token];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(400).send("Invalid password reset token.");
		} else {
			// Check if password reset token is expired
			if (Date.now() > result[0].passwordResetExpires) {
				return response.status(400).send("Password reset link has expired.");
			} else {
				// Hash new password and update password for passwordResetToken and email
				var e = result[0].email;
				bcrypt.hash(newPassword, saltRounds, function(err, hash) {
					sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
					post = [db_accounts, password, hash, passwordResetToken, token, email, e];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.affectedRows != 1) {
							return response.status(500).send("Error resetting password.");
						} else {
							// Set passwordResetExpires to null for passwordResetToken and email
							sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
							post = [db_accounts, passwordResetExpires, null, passwordResetToken, token, email, e];
							dbConnection.query(sql, post, function(err, result) {
								if (err) throw err;
								if (result.affectedRows != 1) {
									return response.status(500).send("Error resetting password.");
								} else {
									// Set passwordResetToken to null for passwordResetToken and email
									sql = "UPDATE ?? SET ??=? WHERE ??=? AND ??=?";
									post = [db_accounts, passwordResetToken, null, passwordResetToken, token, email, e];
									dbConnection.query(sql, post, function(err, result) {
										if (err) throw err;
										if (result.affectedRows != 1) {
											return response.status(500).send("Error resetting password.");
										} else {
											// Send password reset notification email
											const msg = {
												to: e,
												from: supportEmail,
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

// Adds Firebase registration token for current session and user ID
function addFirebaseRegistrationToken(token, request, response) {
	if (request.session.firebaseRegistrationToken) {
		// Delete current Firebase registration token
		var sql = "DELETE FROM ?? WHERE ??=?";
		var post = [db_firebase, registrationToken, request.session.firebaseRegistrationToken];
		dbConnection.query(sql, post, function(err, result){
			if (err) throw err;
			if (result.affectedRows == 1) {
				// Check if Firebase registration token already exists
				var sql = "SELECT ?? FROM ?? WHERE ??=?";
				var post = [registrationToken, db_firebase, registrationToken, token];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					if (result.length != 0) {
						return response.status(500).send("Firebase registration token already exists!");
					} else {
						// Add new Firebase registration token
						var sql = "INSERT INTO ?? SET ??=?, ??=?";
						var post = [db_firebase, registrationToken, token, uid, request.session.uid];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							request.session.firebaseRegistrationToken = token;
							console.log("Firebase registration token updated.");
							return response.status(200).send(JSON.stringify({"response":"pass"}));
						});
					}
				});
			} else {
				return response.status(500).send("Error updating Firebase registration token.");
			}
		});
	} else {
		// Check if Firebase registration token already exists
		var sql = "SELECT ?? FROM ?? WHERE ??=?";
		var post = [registrationToken, db_firebase, registrationToken, token];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			if (result.length != 0) {
				return response.status(500).send("Firebase registration token already exists!");
			} else {
				// Add new Firebase registration token
				var sql = "INSERT INTO ?? SET ??=?, ??=?";
				var post = [db_firebase, registrationToken, token, uid, request.session.uid];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					request.session.firebaseRegistrationToken = token;
					console.log("Firebase registration token added.");
					return response.status(200).send(JSON.stringify({"response":"pass"}));
				});
			}
		});
	}
}

// Updates LinkedIn profile information
function updateLinkedInProfile(e, n, a, response) {
	// Get profile for email
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_profiles, email, e];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(400).send(JSON.stringify({"response":"fail"}));
		} else {
			// Update profile if already exists for email
			var sql = "UPDATE ?? SET ??=?, ??=? WHERE ??=?";
			var post = [db_profiles, name, n, about, a, email, e];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				console.log("LinkedIn profile updated."); 
				return response.status(200).send(JSON.stringify({"response":"pass"}));
			});
		}
	});
}

// Updates profile info
function updateProfile(n, a, i, p, request, response) {
	// Update profile for user ID
	var sql = "UPDATE ?? SET ??=?, ??=?, ??=?, ??=? WHERE ??=?";
	var post = [db_profiles, name, n, about, a, interests, i, picture, p, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result){
		if (err) throw err;
		console.log("Profile info updated.");
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	});
}

// Gets profile information
function getProfile(request, response) {
	// Get profile for user ID and respond with profile data
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_profiles, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length == 0) {
			return response.status(400).send("No profile.");
		} else {
			var n = result[0].name;
			var a = result[0].about;
			var i = result[0].interests;
			var p = result[0].picture;
			return response.status(200).send(JSON.stringify({"response":"pass", name:n, about:a, interests:i, picture:p}));
		}
	});
}

// Updates user location data
function updateLocation(lat, lon, request, response) {
	// Insert uid, longitude, latitude, and time
	var sql = "INSERT INTO ?? SET ??=?, ??=?, ??=?, ??=?";
	var currentTime = Date.now();
	var weekOld = currentTime - 604800000;
	var post = [db_locations, uid, request.session.uid, longitude, lon, latitude, lat, time, currentTime];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		// Delete all location data more than a week old
		var sql = "DELETE FROM ?? WHERE ?? BETWEEN 0 AND ?";
		var post = [db_locations, time, weekOld];
		dbConnection.query(sql, post, function(err, result){
			if (err) throw err;
		});
		response.status(200).send(JSON.stringify({"response":"pass"}));	
		// Get match limit
		var sql = "SELECT ?? FROM ?? WHERE ??=?";
		var post = [matchLimit, db_accounts, uid, request.session.uid];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			var newMatches = 0;
			var edges = matchGraph.outEdges(request.session.uid);
			for (var i = 0; i < edges.length; i++) {
				if (edges[i].name === "newMatch") {
					newMatches++;
				}
			}
			if (newMatches < result[0].matchLimit) {
				// If match limit not reached, update population multiplier and find crossed paths
				updatePopulationMultiplier(lat, lon, currentTime, request);
				findCrossedPaths(lat, lon, currentTime, request);
			}
		});		
	});
}

// Converts feet to degrees latitude
function feetToLat(feet) {
	return feet / 364537.4016;
}

// Converts feet to degrees longitude
function feetToLon(feet, lat) {
	return Math.abs(feet / (364537.4016 * Math.cos(lat)));
}

// Updates population multiplier depending on number of nearby users
function updatePopulationMultiplier(lat, lon, currentTime, request) {
	// Get number of users nearby
	var timeMin = currentTime - CROSS_TIME;
	var latMin = lat - feetToLat(NEARBY_USERS_RADIUS);
	var latMax = lat + feetToLat(NEARBY_USERS_RADIUS);
	var lonMin = lon - feetToLon(NEARBY_USERS_RADIUS, lat);
	var lonMax = lon + feetToLon(NEARBY_USERS_RADIUS, lat);
	sql = "SELECT DISTINCT ?? FROM ?? WHERE ??!=? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ?";
	post = [uid, db_locations, uid, request.session.uid, time, timeMin, currentTime, latitude, latMin, latMax, longitude, lonMin, lonMax];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length < POPULATION_MULTIPLIER_LOW_CUTOFF) {
			// If less than POPULATION_MULTIPLIER_LOW_CUTOFF users in the area
			var sql = "UPDATE ?? SET ??=? WHERE ??=?";
			var post = [db_accounts, populationMultiplier, POPULATION_MULTIPLIER_LOW, uid, request.session.uid];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
			});
		} else if (result.length > POPULATION_MULTIPLIER_HIGH_CUTOFF) {
			// If greater than POPULATION_MULTIPLIER_HIGH_CUTOFF users in the area
			var sql = "UPDATE ?? SET ??=? WHERE ??=?";
			var post = [db_accounts, populationMultiplier, POPULATION_MULTIPLIER_HIGH, uid, request.session.uid];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
			});
		}
	});
}

// Checks if anyone crossed paths
function findCrossedPaths(lat, lon, currentTime, request) {
	// Get cross radius and population multiplier for user ID
	var sql = "SELECT ??, ?? FROM ?? WHERE ??=?";
	var post = [crossRadius, populationMultiplier, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		// Get all user IDs and coordinates within cross time and cross radius
		sql = "SELECT ??, ??, ?? FROM ?? WHERE ??!=? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ?";
		var timeMin = currentTime - CROSS_TIME;
		var latMin = lat - feetToLat(result[0].crossRadius * result[0].populationMultiplier);
		var latMax = lat + feetToLat(result[0].crossRadius * result[0].populationMultiplier);
		var lonMin = lon - feetToLon(result[0].crossRadius * result[0].populationMultiplier, lat);
		var lonMax = lon + feetToLon(result[0].crossRadius * result[0].populationMultiplier, lat);
		post = [uid, latitude, longitude, db_locations, uid, request.session.uid, time, timeMin, currentTime, latitude, latMin, latMax, longitude, lonMin, lonMax];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			// For every result within cross time and cross radius
			for (var i = 0; i < result.length; i++) {
				var uidOther = result[i].uid;
				var latOther = result[i].latitude;
				var lonOther = result[i].longitude;
				if (!matchGraph.hasEdge(request.session.uid, uidOther, "blocked") && !matchGraph.hasEdge(uidOther, request.session.uid, "blocked")) {
					// Get cross radius, match limit, and population multiplier for other user ID
					sql = "SELECT ??, ??, ?? FROM ?? WHERE ??=?";
					post = [crossRadius, matchLimit, populationMultiplier, db_accounts, uid, uidOther];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						var newMatches = 0;
						var edges = matchGraph.outEdges(uidOther);
						for (var i = 0; i < edges.length; i++) {
							if (edges[i].name === "newMatch") {
								newMatches++;
							}
						}
						if (newMatches < result[0].matchLimit) {
							// If other user match limit not reached
							var otherLatMin = latOther - feetToLat(result[0].crossRadius * result[0].populationMultiplier);
							var otherLatMax = latOther + feetToLat(result[0].crossRadius * result[0].populationMultiplier);
							var otherLonMin = lonOther - feetToLon(result[0].crossRadius * result[0].populationMultiplier, latOther);
							var otherLonMax = lonOther + feetToLon(result[0].crossRadius * result[0].populationMultiplier, latOther);
							if (lat >= otherLatMin && lat <= otherLatMax && lon >= otherLonMin && lon <= otherLonMax) {
								// If also within other user ID's cross radius
								if (!matchGraph.hasEdge(request.session.uid, uidOther, "timesCrossed") && !matchGraph.hasEdge(uidOther, request.session.uid, "timesCrossed")) {
									// If never crossed before, create timesCrossed, lastTime, and crossLocations edges
									matchGraph.setEdge(request.session.uid, uidOther, 1, "timesCrossed");
									matchGraph.setEdge(uidOther, request.session.uid, 1, "timesCrossed");
									matchGraph.setEdge(request.session.uid, uidOther, currentTime, "lastTime");
									matchGraph.setEdge(uidOther, request.session.uid, currentTime, "lastTime");
									var object = {};
									var key = "Cross Locations";
									object[key] = [];
									var crossLat = (parseFloat(lat) + parseFloat(latOther)) / parseFloat(2.0);
									var crossLon = (parseFloat(lon) + parseFloat(lonOther)) / parseFloat(2.0);
									var data = {latitude: crossLat, longitude: crossLon};
									object[key].push(data);
									matchGraph.setEdge(request.session.uid, uidOther, JSON.stringify(object), "crossLocations");
									matchGraph.setEdge(uidOther, request.session.uid, JSON.stringify(object), "crossLocations");
									console.log('Users crossed paths for the first time.');
								} else if (matchGraph.edge(request.session.uid, uidOther, "lastTime") < currentTime - CROSS_COOLDOWN && matchGraph.edge(uidOther, request.session.uid, "lastTime") < currentTime - CROSS_COOLDOWN) {
									// If crossed before, increment timesCrossed edge and update lastTime and crossLocations edges
									matchGraph.setEdge(request.session.uid, uidOther, matchGraph.edge(request.session.uid, uidOther, "timesCrossed") + 1, "timesCrossed");
									matchGraph.setEdge(uidOther, request.session.uid, matchGraph.edge(uidOther, request.session.uid, "timesCrossed") + 1, "timesCrossed");
									matchGraph.setEdge(request.session.uid, uidOther, currentTime, "lastTime");
									matchGraph.setEdge(uidOther, request.session.uid, currentTime, "lastTime");
									var object = JSON.parse(matchGraph.edge(request.session.uid, uidOther, "crossLocations"));
									var key = "Cross Locations";
									var crossLat = (parseFloat(lat) + parseFloat(latOther)) / parseFloat(2.0);
									var crossLon = (parseFloat(lon) + parseFloat(lonOther)) / parseFloat(2.0);
									var data = {latitude: crossLat, longitude: crossLon};
									object[key].push(data);
									matchGraph.setEdge(request.session.uid, uidOther, JSON.stringify(object), "crossLocations");
									matchGraph.setEdge(uidOther, request.session.uid, JSON.stringify(object), "crossLocations");
									console.log('Users crossed paths again.');
								}
								if (matchGraph.edge(request.session.uid, uidOther, "timesCrossed") >= MATCH_THRESHOLD && matchGraph.edge(uidOther, request.session.uid, "timesCrossed") >= MATCH_THRESHOLD && !matchGraph.hasEdge(request.session.uid, uidOther, "matched") && !matchGraph.hasEdge(uidOther, request.session.uid, "matched") && !matchGraph.hasEdge(request.session.uid, uidOther, "newMatch") && !matchGraph.hasEdge(uidOther, request.session.uid, "newMatch")) {
									if (!matchGraph.hasEdge(request.session.uid, uidOther, "unmatched") && !matchGraph.hasEdge(uidOther, request.session.uid, "unmatched")) {
										// If crossed greater than or equal to match threshold times and not unmatched and not already matched, create newMatch edges
										matchGraph.setEdge(request.session.uid, uidOther, true, "newMatch");
										matchGraph.setEdge(uidOther, request.session.uid, true, "newMatch");
										console.log('Users matched.');
									} else if (matchGraph.edge(request.session.uid, uidOther, "timesCrossed") >= UNMATCHED_MATCH_THRESHOLD && matchGraph.edge(uidOther, request.session.uid, "timesCrossed") >= UNMATCHED_MATCH_THRESHOLD) {
										// If crossed greater than or equal to unmatched match threshold times and unmatched and not already matched, create newMatch edges
										matchGraph.setEdge(request.session.uid, uidOther, true, "newMatch");
										matchGraph.setEdge(uidOther, request.session.uid, true, "newMatch");
										if (matchGraph.hasEdge(request.session.uid, uidOther, "unmatched")) {
											matchGraph.removeEdge(request.session.uid, uidOther, "unmatched");
										}
										if (matchGraph.hasEdge(uidOther, request.session.uid, "unmatched")) {
											matchGraph.removeEdge(uidOther, request.session.uid, "unmatched");
										}
										console.log('Unmatched users matched.');
									}
								} else if (matchGraph.edge(request.session.uid, uidOther, "timesCrossed") >= MATCH_THRESHOLD && matchGraph.edge(uidOther, request.session.uid, "timesCrossed") >= MATCH_THRESHOLD && matchGraph.hasEdge(request.session.uid, uidOther, "matched") && matchGraph.hasEdge(uidOther, request.session.uid, "matched") && matchGraph.hasEdge(request.session.uid, uidOther, "approved") && matchGraph.hasEdge(uidOther, request.session.uid, "approved") && matchGraph.edge(request.session.uid, uidOther, "approved") == true && matchGraph.edge(uidOther, request.session.uid, "approved") == true) {
									// If crossed and already matched, notify users
									var sql = "SELECT ?? FROM ?? WHERE ??=?";
									var post = [registrationToken, db_firebase, uid, request.session.uid];
									dbConnection.query(sql, post, function(err, result) {
										if (err) throw err;
										if (result.length > 0) {
											for (var i = 0; i < result.length; i++) {
												var message = {
													data: {
														type: 'Crossed Paths',
														title: 'You just crossed paths with one of your matches!',
														body: 'Tap to see who you crossed paths with.',
														uid: uidOther
													},
													token: result[i].registrationToken,
													android: {
														ttl: 3600000,
														priority: 'high',
													}
												};
												admin.messaging().send(message)
													.then((response) => {
														console.log('Successfully sent crossed paths notification.');
													})
													.catch((error) => {
														console.log(error);
													});
											}
										}
									});
									var sql = "SELECT ?? FROM ?? WHERE ??=?";
									var post = [registrationToken, db_firebase, uid, uidOther];
									dbConnection.query(sql, post, function(err, result) {
										if (err) throw err;
										if (result.length > 0) {
											for (var i = 0; i < result.length; i++) {
												var message = {
													data: {
														type: 'Crossed Paths',
														title: 'You just crossed paths with one of your matches!',
														body: 'Tap to see who you crossed paths with.',
														uid: request.session.uid
													},
													token: result[i].registrationToken,
													android: {
														ttl: 3600000,
														priority: 'high',
													}
												};
												admin.messaging().send(message)
													.then((response) => {
														console.log('Successfully sent crossed paths notification.');
													})
													.catch((error) => {
														console.log(error);
													});
											}
										}
									});
								}
							}
						}

					});
				}
			}
			writeMatchGraph();
		});
	});
}

// Notifies all users that have new matches and removes newMatch edges
function notifyMatches() {
	var edges = matchGraph.edges();
	var toNotify = {};
	for (var i = 0; i < matchGraph.edgeCount(); i++) {
		// Find all new matches
		if (edges[i] != null && edges[i].name === "newMatch") {
			if (toNotify.hasOwnProperty(edges[i].v)) {
				var index = toNotify[edges[i].v].indexOf(edges[i].w);
				if (index == -1) {
					toNotify[edges[i].v].push(edges[i].w);
				}
			} else {
				toNotify[edges[i].v] = [];
				toNotify[edges[i].v].push(edges[i].w);
			}
		}
	}
	for (var key in toNotify) {
		if (toNotify.hasOwnProperty(key)) {
			if (toNotify[key].length == 1) {
				// Create matched and approved edges and remove newMatch edge
				matchGraph.setEdge(key, toNotify[key][0], true, "matched");
				matchGraph.setEdge(key, toNotify[key][0], false, "approved");
				matchGraph.removeEdge(key, toNotify[key][0], "newMatch");
				compareMatchInterests(key, toNotify[key][0]);
			} else if (toNotify[key].length > 1) {
				for (var j = 0; j < toNotify[key].length; j++) {
					// Create matched and approved edges and remove newMatch edge
					matchGraph.setEdge(key, toNotify[key][j], true, "matched");
					matchGraph.setEdge(key, toNotify[key][j], false, "approved");
					matchGraph.removeEdge(key, toNotify[key][j], "newMatch");
				}
				// Notify user of multiple matches
				var sql = "SELECT ?? FROM ?? WHERE ??=?";
				var post = [registrationToken, db_firebase, uid, key];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					if (result.length > 0) {
						for (var j = 0; j < result.length; j++) {
							var message = {
								data: {
									type: 'New Matches',
									title: 'You have new matches!',
									body: 'Tap to see your matches.',
									uid: ''
								},
								token: result[j].registrationToken,
								android: {
									ttl: 3600000,
									priority: 'high'
								}
							};
							admin.messaging().send(message)
								.then((response) => {
									console.log('Successfully sent new matches notification.');
								})
								.catch((error) => {
									console.log(error);
								});
						}
					}
				});
			}
		}
	}
	writeMatchGraph();
}

// Notifies all users that have no new matches
function notifyNoMatches() {
	var nodes = matchGraph.nodes();
	for (var i = 0; i < nodes.length; i++) {
		// Check for users without matches
		var edges = matchGraph.outEdges(nodes[i]);
		var hasMatches = false;
		for (var j = 0; j < edges.length; j++) {
			if (edges[j].name === "matched" || edges[j].name === "newMatch") {
				hasMatches = true;
				break;
			}
		}
		if (!hasMatches) {
			// Notify user of no matches and suggest locations
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [registrationToken, db_firebase, uid, nodes[i]];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length > 0) {
					for (var j = 0; j < result.length; j++) {
						var message = {
							data: {
								type: 'Location Suggestions',
								title: 'You didn\'t match with anyone.',
								body: 'Tap the map options menu to see suggested locations.'
							},
							token: result[j].registrationToken,
							android: {
								ttl: 3600000,
								priority: 'high'
							}
						};
						admin.messaging().send(message)
							.then((response) => {
								console.log('Successfully sent location suggestions notification.');
							})
							.catch((error) => {
								console.log(error);
							});
					}
				}
			});
		}
	}
}

// Determines if users who matched share interests
function compareMatchInterests(firstUid, secondUid) {
	// Get interests
	var sql = "SELECT ?? FROM ?? WHERE ??=? OR ??=?";
	var post = [interests, db_profiles, uid, firstUid, uid, secondUid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		var firstInterests = result[0].interests;
		var secondInterests = result[1].interests;
		if (firstInterests !== "No Interests" && secondInterests !== "No Interests") {
			// Compare interests
			firstInterests = firstInterests.split(', ');
			secondInterests = secondInterests.split(', ');
			for (var i = 0; i < firstInterests.length; i++) {
				for (var j = 0; j < secondInterests.length; j++) {
					if (firstInterests[i] === secondInterests[j]) {
						notifyMatchSharedInterests(firstUid, secondUid);
						return;
					}
				}
			}
		}
		notifyMatchNoSharedInterests(firstUid, secondUid);
	});
}

// Notifies specific user that matched
function notifyMatchNoSharedInterests(firstUid, secondUid) {
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [registrationToken, db_firebase, uid, firstUid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length > 0) {
			for (var i = 0; i < result.length; i++) {
				var message = {
					data: {
						type: 'New Matches',
						title: 'You have a new match!',
						body: 'Tap to see who you matched with.',
						uid: secondUid
					},
					token: result[i].registrationToken,
					android: {
						ttl: 3600000,
						priority: 'high'
					}
				};
				admin.messaging().send(message)
					.then((response) => {
						console.log('Successfully sent new match notification.');
					})
					.catch((error) => {
						console.log(error);
					});
			}
		}
	});
}

// Notifies specific user that matched and has shared interests
function notifyMatchSharedInterests(firstUid, secondUid) {
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [registrationToken, db_firebase, uid, firstUid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length > 0) {
			for (var i = 0; i < result.length; i++) {
				var message = {
					data: {
						type: 'New Matches',
						title: 'You have a new match with shared interests!',
						body: 'Tap to see who you matched with.',
						uid: secondUid
					},
					token: result[i].registrationToken,
					android: {
						ttl: 3600000,
						priority: 'high'
					}
				};
				admin.messaging().send(message)
					.then((response) => {
						console.log('Successfully sent new match with shared interests notification.');
					})
					.catch((error) => {
						console.log(error);
					});
			}
		}
	});
}

// Approves the user with the given user ID
function approveUser(u, request, response) {
	matchGraph.setEdge(request.session.uid, u, true, "approved");
	writeMatchGraph();
	// Notify approved user
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [registrationToken, db_firebase, uid, u];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length > 0) {
			for (var i = 0; i < result.length; i++) {
				var message = {
					data: {
						type: 'Match Approval',
						title: 'You were just approved by one of your matches!',
						body: 'Tap to see who approved you.',
						uid: request.session.uid
					},
					token: result[i].registrationToken,
					android: {
						ttl: 3600000,
						priority: 'high',
					}
				};
				admin.messaging().send(message)
					.then((response) => {
						console.log('Successfully sent match approval notification.');
					})
					.catch((error) => {
						console.log(error);
					});
			}
		}
	});
	console.log('User approved.');
	return response.status(200).send(JSON.stringify({"response":"pass"}));
}

// Unapproves the user with the given user ID
function unapproveUser(u, request, response) {
	matchGraph.setEdge(request.session.uid, u, false, "approved");
	writeMatchGraph();
	console.log('User unapproved.');
	return response.status(200).send(JSON.stringify({"response":"pass"}));
}

// Unmatches the user with the given user ID
function unmatchUser(u, request, response) {
	matchGraph.setEdge(request.session.uid, u, true, "unmatched");
	matchGraph.removeEdge(request.session.uid, u, "matched");
	matchGraph.removeEdge(u, request.session.uid, "matched");
	matchGraph.removeEdge(request.session.uid, u, "approved");
	matchGraph.removeEdge(u, request.session.uid, "approved");
	matchGraph.removeEdge(request.session.uid, u, "timesCrossed");
	matchGraph.removeEdge(u, request.session.uid, "timesCrossed");
	matchGraph.removeEdge(request.session.uid, u, "lastTime");
	matchGraph.removeEdge(u, request.session.uid, "lastTime");
	matchGraph.removeEdge(request.session.uid, u, "crossLocations");
	matchGraph.removeEdge(u, request.session.uid, "crossLocations");
	writeMatchGraph();
	// Delete messages
	var sql = "DELETE FROM ?? WHERE ??=? AND ??=? OR ??=? AND ??=?";
	var post = [db_messages, uidFrom, request.session.uid, uidTo, u, uidFrom, u, uidTo, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		console.log('User unmatched.');
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	});
}

// Blocks the user with the given user ID
function blockUser(u, request, response) {
	matchGraph.setEdge(request.session.uid, u, true, "blocked");
	matchGraph.removeEdge(request.session.uid, u, "matched");
	matchGraph.removeEdge(u, request.session.uid, "matched");
	matchGraph.removeEdge(request.session.uid, u, "approved");
	matchGraph.removeEdge(u, request.session.uid, "approved");
	matchGraph.removeEdge(request.session.uid, u, "timesCrossed");
	matchGraph.removeEdge(u, request.session.uid, "timesCrossed");
	matchGraph.removeEdge(request.session.uid, u, "lastTime");
	matchGraph.removeEdge(u, request.session.uid, "lastTime");
	matchGraph.removeEdge(request.session.uid, u, "crossLocations");
	matchGraph.removeEdge(u, request.session.uid, "crossLocations");
	writeMatchGraph();
	// Delete messages
	var sql = "DELETE FROM ?? WHERE ??=? AND ??=? OR ??=? AND ??=?";
	var post = [db_messages, uidFrom, request.session.uid, uidTo, u, uidFrom, u, uidTo, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		console.log('User blocked.');
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	});
}

// Unblocks the user with the given user ID
function unblockUser(u, request, response) {
	if (matchGraph.hasEdge(request.session.uid, u, "blocked") && matchGraph.edge(request.session.uid, u, "blocked") === true) {
		matchGraph.removeEdge(request.session.uid, u, "blocked");
		writeMatchGraph();
		console.log('User unblocked.');
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	}
}

// Reports the user with the given user ID
function reportUser(u, r, request, response) {
	matchGraph.setEdge(request.session.uid, u, true, "blocked");
	matchGraph.removeEdge(request.session.uid, u, "matched");
	matchGraph.removeEdge(u, request.session.uid, "matched");
	matchGraph.removeEdge(request.session.uid, u, "approved");
	matchGraph.removeEdge(u, request.session.uid, "approved");
	matchGraph.removeEdge(request.session.uid, u, "timesCrossed");
	matchGraph.removeEdge(u, request.session.uid, "timesCrossed");
	matchGraph.removeEdge(request.session.uid, u, "lastTime");
	matchGraph.removeEdge(u, request.session.uid, "lastTime");
	matchGraph.removeEdge(request.session.uid, u, "crossLocations");
	matchGraph.removeEdge(u, request.session.uid, "crossLocations");
	writeMatchGraph();
	// Delete messages
	var sql = "DELETE FROM ?? WHERE ??=? AND ??=? OR ??=? AND ??=?";
	var post = [db_messages, uidFrom, request.session.uid, uidTo, u, uidFrom, u, uidTo, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		// Add offense to offenses table
		var sql = "INSERT INTO ?? SET ??=?, ??=?";
		var post = [db_offenses, uid, u, reason, r];
		dbConnection.query(sql, post, function(err, result){
			if (err) throw err;
			// Get total number of offenses
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [uid, db_offenses, uid, u];
			dbConnection.query(sql, post, function(err, result){
				if (result.length == WARN_THRESHOLD) {
					// Warn user of offenses
					var sql = "SELECT ?? FROM ?? WHERE ??=?";
					var post = [registrationToken, db_firebase, uid, u];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.length > 0) {
							for (var i = 0; i < result.length; i++) {
								var message = {
									data: {
										type: 'Offense Warning',
										title: 'You have been reported multiple times.',
										body: 'Your account will be banned if you continue to be reported.',
									},
									token: result[i].registrationToken,
									android: {
										ttl: 3600000,
										priority: 'high',
									}
								};
								admin.messaging().send(message)
									.then((response) => {
										console.log('Successfully sent offense warning notification.');
									})
									.catch((error) => {
										console.log(error);
									});
							}
						}
					});
				} else if (result.length >= BAN_THRESHOLD) {
					// Update account banned to true
					var sql = "UPDATE ?? SET ??=? WHERE ??=?";
					var post = [db_accounts, banned, true, uid, u];
					dbConnection.query(sql, post, function(err, result){
						if (err) throw err;
						matchGraph.removeNode(u);
						matchGraph.setNode(u);
						writeMatchGraph();
						console.log('User banned.');
					});
				}
				// Send report notification email
				const msg = {
					to: supportEmail,
					from: supportEmail,
					subject: 'Offense Report',
					text: 'USER ID: ' + u + ' REASON: ' + r,
					html: '<strong>USER ID: ' + u + ' REASON: ' + r + '</strong>'
				};
				sgMail.send(msg);
				console.log('User reported.');
				return response.status(200).send(JSON.stringify({"response":"pass"}));
			});
		});
	});
}

// Gets all location coordinates for user ID for heatmap generation
function getLocationsForHeatmap(request, response) {
	var sql = "SELECT ??,?? FROM ?? WHERE ??=?";
	var post = [latitude, longitude, db_locations, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result){
		var object = {};
		var key = "Location";
		object[key] = [];
		for (var i = 0; i < result.length; i++) {
			var lat = result[i].latitude;
			var lon = result[i].longitude;
			var data = {latitude: lat, longitude: lon};
			object[key].push(data);
		}
		console.log("User locations for heatmap sent.");
		return response.status(200).send(JSON.stringify(object));
	});
}

// Gets all location coordinates for all users for heatmap generation
function getAllLocationsForHeatmap(request, response) {
	var sql = "SELECT ??,?? FROM ??";
	var post = [latitude, longitude, db_locations];
	dbConnection.query(sql, post, function(err, result){
		var object = {};
		var key = "Location";
		object[key] = [];
		for (var i = 0; i < result.length; i++) {
			var lat = result[i].latitude;
			var lon = result[i].longitude;
			var data = {latitude: lat, longitude: lon};
			object[key].push(data);
		}
		console.log("All locations for heatmap sent.");
		return response.status(200).send(JSON.stringify(object));
	});
}

// Gets user IDs of all matches
function getAllMatches(request, response) {
	var edges = matchGraph.outEdges(request.session.uid);
	var object = {};
	var key = "UIDs";
	object[key] = [];
	for (var i = 0; i < edges.length; i++) {
		if (edges[i].name === "matched") {
			var data = {uid: edges[i].w};
			object[key].push(data);
		}
	}
	return response.status(200).send(JSON.stringify(object));
}

// Gets information of a single match
function getMatch(u, request, response) {
	if (matchGraph.hasEdge(request.session.uid, u, "matched") && matchGraph.edge(request.session.uid, u, "matched") === true) {
		var sql = "SELECT * FROM ?? WHERE ??=?";
		var post = [db_profiles, uid, u];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			if (result.length != 1) {
				return response.status(500).send("Error getting match information.\n");
			} else {
				var object = {};
				var key = "Profile";
				object[key] = [];
				var n = result[0].name;
				var a = result[0].about;
				var i = result[0].interests;
				var p = result[0].picture;
				var t = matchGraph.edge(request.session.uid, u, "timesCrossed");
				var ap = matchGraph.edge(request.session.uid, u, "approved");
				var data = {uid: u, name: n, about: a, interests: i, picture: p, timesCrossed: t, approved: ap};
				object[key].push(data);
				return response.status(200).send(JSON.stringify(object));
			}
		});
	}
}

// Gets user IDs of all blocked users
function getAllBlocked(request, response) {
	var edges = matchGraph.outEdges(request.session.uid);
	var object = {};
	var key = "UIDs";
	object[key] = [];
	for (var i = 0; i < edges.length; i++) {
		if (edges[i].name === "blocked") {
			var data = {uid: edges[i].w};
			object[key].push(data);
		}
	}
	return response.status(200).send(JSON.stringify(object));
}

// Gets information of a single blocked user
function getBlocked(u, request, response) {
	if (matchGraph.hasEdge(request.session.uid, u, "blocked") && matchGraph.edge(request.session.uid, u, "blocked") === true) {
		var sql = "SELECT ??,?? FROM ?? WHERE ??=?";
		var post = [name, picture, db_profiles, uid, u];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			if (result.length != 1) {
				return response.status(500).send("Error getting blocked user's information.\n");
			} else {
				var object = {};
				var key = "Blocked";
				object[key] = [];
				var n = result[0].name;
				var p = result[0].picture;
				var data = {uid: u, name: n, picture: p};
				object[key].push(data);
				return response.status(200).send(JSON.stringify(object));
			}
		});
	}
}

// Gets all location coordinates where users crossed paths
function getCrossLocations(u, request, response) {
	if (matchGraph.hasEdge(request.session.uid, u, "crossLocations") && matchGraph.hasEdge(u, request.session.uid, "crossLocations") && matchGraph.hasEdge(request.session.uid, u, "approved") && matchGraph.hasEdge(u, request.session.uid, "approved") && matchGraph.edge(request.session.uid, u, "approved") == true && matchGraph.edge(u, request.session.uid, "approved") == true) {
		console.log("Cross locations sent.");
		return response.status(200).send(matchGraph.edge(request.session.uid, u, "crossLocations"));
	}
	return response.status(400).send("Not approved.");
}

// Gets all messages for chat with a match
function getMessages(u, request, response) {
	var sql = "SELECT * FROM ?? WHERE ??=? AND ??=? OR ??=? AND ??=? ORDER BY ?? LIMIT 1000";
	var post = [db_messages, uidFrom, request.session.uid, uidTo, u, uidFrom, u, uidTo, request.session.uid, time];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		var object = {};
		var key = "Messages";
		object[key] = [];
		for (var i = 0; i < result.length; i++) {
			object[key].push({uidFrom: result[i].uidFrom, uidTo: result[i].uidTo, message: result[i].message, time: result[i].time});
		}
		return response.status(200).send(JSON.stringify(object));
	});
}

// Stores a location tag
function storeTagData(t, request, response) {
	var tag = JSON.parse(t);
	// Validate tag coordinates, title, and description
	if (!validateCoordinates(tag.latitude.toString(), tag.longitude.toString())) {
		return response.status(400).send("Invalid tag coordinates.");
	}
	if (!validateTagTitle(tag.title)) {
		return response.status(400).send(JSON.stringify({"response":"Invalid tag title"}));
	}
	if (!validateTagDescription(tag.description)) {
		return response.status(400).send(JSON.stringify({"response":"Invalid tag description"}));
	}
	var sql = "INSERT INTO ?? (??,??,??,??,??) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE ??=?, ??=?";
	var post = [db_tags, uid, latitude, longitude, title, description, request.session.uid, tag.latitude, tag.longitude, tag.title, tag.description, title, tag.title, description, tag.description];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	});
}

// Gets all tags for user
function getTagData(request, response) {
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_tags, uid, request.session.uid]; 
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		var object = {};
		var key = "Tags";
		object[key] = [];
		for (var i = 0; i < result.length; i++) {
			var data = {latitude: result[i].latitude, longitude: result[i].longitude, title: result[i].title, description: result[i].description};
			object[key].push(data);
		}
		console.log("User tags sent.");
		return response.status(200).send(JSON.stringify(object));
	});
}

// Gets tags of matched user
function getMatchTagData(u, request, response) {
	if (matchGraph.hasEdge(u, request.session.uid, "approved") && matchGraph.hasEdge(request.session.uid, u, "approved") && matchGraph.edge(u, request.session.uid, "approved") === true && matchGraph.edge(request.session.uid, u, "approved") === true) {
		var sql = "SELECT * FROM ?? WHERE ??=?";
		var post = [db_tags, uid, u]; 
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			var object = {};
			var key = "Tags";
			object[key] = [];
			for (var i = 0; i < result.length; i++) {
				var data = {latitude: result[i].latitude, longitude: result[i].longitude, title: result[i].title, description: result[i].description};
				object[key].push(data);
			}
			return response.status(200).send(JSON.stringify(object));
		});
	} else {
		return response.status(400).send("Not approved.");
	}
}

// Deletes a location tag
function deleteTagData(t, request, response) {
	var tag = JSON.parse(t);
	// Validate tag coordinates, title, and description
	if (!validateCoordinates(tag.latitude.toString(), tag.longitude.toString())) {
		return response.status(400).send("Invalid tag coordinates.");
	}
	if (!validateTagTitle(tag.title)) {
		return response.status(400).send("Invalid tag title");
	}
	if (!validateTagDescription(tag.description)) {
		return response.status(400).send("Invalid tag description");
	}
	var sql = "DELETE FROM ?? WHERE ??=? AND ??=? AND ??=? AND ??=? AND ??=?";
	var post = [db_tags, uid, request.session.uid, latitude, tag.latitude, longitude, tag.longitude, title, tag.title, description, tag.description];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	});
}

// Gets statistics about users, matches, and location
function getStatistics(lat, lon, request, response) {
	var statistics = {};
	// Get number of users
	var sql = "SELECT * FROM ??";
	var post = [db_accounts];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		statistics['numUsers'] = result.length;
		// Get number of users nearby
		var currentTime = Date.now();
		var timeMin = currentTime - CROSS_TIME;
		var latMin = lat - feetToLat(NEARBY_USERS_RADIUS);
		var latMax = lat + feetToLat(NEARBY_USERS_RADIUS);
		var lonMin = lon - feetToLon(NEARBY_USERS_RADIUS, lat);
		var lonMax = lon + feetToLon(NEARBY_USERS_RADIUS, lat);
		sql = "SELECT DISTINCT ?? FROM ?? WHERE ??!=? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ?";
		post = [uid, db_locations, uid, request.session.uid, time, timeMin, currentTime, latitude, latMin, latMax, longitude, lonMin, lonMax];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			statistics['numNearYou'] = result.length;
			// Get number of matches and new matches
			var edges = matchGraph.edges();
			var numMatches = 0;
			var numNewMatches = 0;
			for (var i = 0; i < matchGraph.edgeCount(); i++) {
				if (edges[i] != null && edges[i].name === "matched") {
					numMatches++;
				} else if (edges[i] != null && edges[i].name === "newMatch") {
					numNewMatches++;
				}
			}
			statistics['numMatches'] = numMatches / 2;
			statistics['numNewMatches'] = numNewMatches / 2;
			console.log('Statistics sent.');
			return response.status(200).send(JSON.stringify(statistics));
		});
	});
}

// Gets suggested locations with heavy user activity
function getLocationSuggestions(request, response) {
	return response.status(200).send(JSON.stringify(popularLocations));
}

// Updates popular locations and writes them to a file
function updatePopularLocations() {
	var sql = "SELECT ??,?? FROM ??";
	var post = [latitude, longitude, db_locations];
	dbConnection.query(sql, post, function(err, result) {
		if (result.length > 0) {
			var coordinates = [];
			for (var i = 0; i < result.length; i++) {
				coordinates.push([result[i].latitude, result[i].longitude]);
			}
			var bias = 5;
			popularLocations = geocluster(coordinates, bias);
			console.log('Popular locations updated.');
		}
	});
}

// Notifies matches who have the same interests
function notifyInterestsChange(request, response) {
	var edges = matchGraph.outEdges(request.session.uid);
	for (var i = 0; i < edges.length; i++) {
		if (edges[i].name === "matched") {
			// Get interests
			var sql = "SELECT ??,?? FROM ?? WHERE ??=? OR ??=?";
			var post = [uid, interests, db_profiles, uid, request.session.uid, uid, edges[i].w];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				var interests;
				var matchInterests;
				var matchUid;
				if (result[0].uid === request.session.uid) {
					interests = result[0].interests;
					matchInterests = result[1].interests;
					matchUid = result[1].uid;
				} else if (result[1].uid === request.session.uid) {
					interests = result[1].interests;
					matchInterests = result[0].interests;
					matchUid = result[0].uid;
				}
				if (interests !== "No Interests" && matchInterests !== "No Interests") {
					// Compare interests
					interests = interests.split(', ');
					matchInterests = matchInterests.split(', ');
					var shared = false;
					for (var j = 0; j < matchInterests.length; j++) {
						for (var k = 0; k < interests.length; k++) {
							if (matchInterests[j] === interests[k]) {
								shared = true;
							}
						}
					}
					if (shared) {
						// Notify user of shared interests
						var sql = "SELECT ?? FROM ?? WHERE ??=?";
						var post = [registrationToken, db_firebase, uid, request.session.uid];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							if (result.length > 0) {
								for (var j = 0; j < result.length; j++) {
									var message = {
										data: {
											type: 'Shared Interests',
											title: 'You share interests with one of your matches!',
											body: 'Tap to see who you share interests with.',
											uid: matchUid
										},
										token: result[j].registrationToken,
										android: {
											ttl: 3600000,
											priority: 'high'
										}
									};
									admin.messaging().send(message)
										.then((response) => {
											console.log('Successfully sent shared interests notification.');
										})
										.catch((error) => {
											console.log(error);
										});
								}
							}
						});
						// Notify match of shared interests
						var sql = "SELECT ?? FROM ?? WHERE ??=?";
						var post = [registrationToken, db_firebase, uid, matchUid];
						dbConnection.query(sql, post, function(err, result) {
							if (err) throw err;
							if (result.length > 0) {
								for (var j = 0; j < result.length; j++) {
									var message = {
										data: {
											type: 'Shared Interests',
											title: 'You share interests with one of your matches!',
											body: 'Tap to see who you share interests with.',
											uid: request.session.uid
										},
										token: result[j].registrationToken,
										android: {
											ttl: 3600000,
											priority: 'high'
										}
									};
									admin.messaging().send(message)
										.then((response) => {
											console.log('Successfully sent shared interests notification.');
										})
										.catch((error) => {
											console.log(error);
										});
								}
							}
						});
					}
				}
			});
		}
	}
	return response.status(200).send(JSON.stringify({"response":"pass"}));
}

// Writes the match graph to a file
function writeMatchGraph() {
	fs.writeFileSync('matchGraph.json', JSON.stringify(graphlib.json.write(matchGraph)));
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
