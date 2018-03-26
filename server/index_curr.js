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

// Load environment variables
require('dotenv').config();

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
const confirmed = "confirmed";
const passwordResetToken = "passwordResetToken";
const passwordResetExpires = "passwordResetExpires";
const crossRadius = "crossRadius";
const latitude = "latitude";
const longitude = "longitude";
const time = "time";
const firstName = "firstName";
const lastName = "lastName";
const loc = "loc";
const about = "about";
const interests = "interests";
const picture = "picture";
const registrationToken = "registrationToken";
const googleID = "googleID";
const facebookID = "facebookID";
const timesCrossed = "timesCrossed";
const approved = "approved";

// Constants used for MySQL
const db_host = process.env.DB_HOST;
const db_username = process.env.DB_USERNAME;
const db_password = process.env.DB_PASSWORD;
const db_name = "wander";
const db_accounts = "accounts";
const db_profiles = "profiles";
const db_locations = "locations";
const db_firebase = "firebase";

// Constant used for password hashing
const saltRounds = 10;

// Constants used for matching
//const MATCH_THRESHOLD = 10;			// 10 crossed paths
const MATCH_THRESHOLD = 1;			// 1 crossed paths (for testing purposes only)
const DEFAULT_CROSS_RADIUS = 150;		// 150 feet
const CROSS_TIME = 30000;			// 30 seconds
//const CROSS_COOLDOWN = 1800000;		// 30 minutes
const CROSS_COOLDOWN = 1000;			// 1 second (for testing purposes only)
//const MATCH_NOTIFY_CRON = '0 20 * * * *';	// every day at 20:00
const MATCH_NOTIFY_CRON = '0,5 * * * * *'

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

// Setup express
var app = express();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
	extended: true
}));

// Setup session
app.set('trust proxy', 1);
app.use(session({
	name: 'wander-cookie',
	secret: crypto.randomBytes(16).toString('hex'),
	resave: false,
	saveUninitialized: true,
	cookie: { domain: '.vvander.me', httpOnly: true, secure: true, maxAge: 31536000000 }
}));

// Create connection to MySQL database
var dbConnection = mysql.createConnection({
	host: db_host,
	user: db_username,
	password: db_password,
	database: db_name
});

// Setup server
var matchGraph;
startServer();

// Setup schedule for notifying users who have matched
schedule.scheduleJob(MATCH_NOTIFY_CRON, function() {
	notifyMatches();
});

// Starts the server
function startServer() {
	if (fs.existsSync('matchGraph.json')) {
		// Read existing match graph
		matchGraph = graphlib.json.read(JSON.parse(fs.readFileSync('matchGraph.json')));
		console.log('Match graph read.');
		// Delete all Firebase registration tokens
		var sql = "TRUNCATE TABLE ??";
		var post = [db_firebase];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			console.log('Firebase registration tokens deleted.');
			// Start HTTP server
			var httpServer = http.createServer(app);
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
			// Delete all Firebase registration tokens
			var sql = "TRUNCATE TABLE ??";
			var post = [db_firebase];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				console.log('Firebase registration tokens deleted.');
				// Start HTTP server
				var httpServer = http.createServer(app);
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

	// Validate username, password, and email
	if (validateUsername(request.body.username)) {
		var u = request.body.username;
	} else {
		return response.status(400).send("Username must be alphanumeric and have a minimum length of 4 characters and a maximum length of 24 characters.\n");
	}
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send("Password must only contain ASCII characters and must have a minimum length of 8 characters and a maximum length of 64 characters.\n");
	}
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send("Email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.\n");
	}

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
	if (request.session && ((request.session.authenticated && request.session.authenticated === true) || (request.session.googleAuthenticated && request.session.googleAuthenticated === true) || (request.session.facebookAuthenticated && request.session.facebookAuthenticated === true))) {
		return response.status(400).send("User already logged in.\n");
	}

	// Validate username and password
	if (validateUsername(request.body.username)) {
		var u = request.body.username;
	} else {
		return response.status(400).send("Invalid username or password.\n");
	}
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send("Invalid username or password.\n");
	}

	login(u, p, request, response);
});

// Called when a POST request is made to /googleLogin
app.post('/googleLogin', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (googleID and email)
	if (Object.keys(request.body).length != 2 || !request.body.googleID || !request.body.email) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session already authenticated
	if (request.session && ((request.session.authenticated && request.session.authenticated === true) || (request.session.googleAuthenticated && request.session.googleAuthenticated === true) || (request.session.facebookAuthenticated && request.session.facebookAuthenticated === true))) {
		return response.status(400).send("User already logged in.\n");
	}

	var id = request.body.googleID;
	// Validate email
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send("Email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.\n");
	}

	googleLogin(id, e, request, response);
});

// Called when a POST request is made to /facebookLogin
app.post('/facebookLogin', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (facebookID and email)
	if (Object.keys(request.body).length != 2 || !request.body.facebookID || !request.body.email) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session already authenticated
	if (request.session && ((request.session.authenticated && request.session.authenticated === true) || (request.session.googleAuthenticated && request.session.googleAuthenticated === true) || (request.session.facebookAuthenticated && request.session.facebookAuthenticated === true))) {
		return response.status(400).send("User already logged in.\n");
	}

	var id = request.body.facebookID;
	// Validate email
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send("Email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.\n");
	}

	facebookLogin(id, e, request, response);
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
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
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
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send(JSON.stringify({"response":"fail"}));
	}

	if (request.session && request.session.googleAuthenticated && request.session.googleAuthenticated === true) {
		return response.status(200).send(JSON.stringify({"response":"google"}));
	} else if (request.session && request.session.facebookAuthenticated && request.session.facebookAuthenticated === true) {
		return response.status(200).send(JSON.stringify({"response":"facebook"}));
	} else {
		return response.status(200).send(JSON.stringify({"response":"pass"}));
	}
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

	// Validate password
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send("Invalid password.\n");
	}

	deleteAccount(p, request, response);
});

// Called when a POST request is made to /googleDeleteAccount
app.post('/googleDeleteAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.googleAuthenticated || request.session.googleAuthenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	googleDeleteAccount(request, response);
});

// Called when a POST request is made to /facebookDeleteAccount
app.post('/facebookDeleteAccount', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.facebookAuthenticated || request.session.facebookAuthenticated === false) {
		return response.status(400).send("User not logged in.\n");
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
		return response.status(400).send("Invalid email.\n");
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
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || !request.session.authenticated || request.session.authenticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate password and newUsername
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send("Invalid password.\n");
	}
	if (validateUsername(request.body.newUsername)) {
		var n = request.body.newUsername;
	} else {
		return response.status(400).send("New username must be alphanumeric and have a minimum length of 4 characters and a maximum length of 24 characters.\n");
	}

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

	// Validate password and newPassword
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send("Invalid password.\n");
	}
	if (validatePassword(request.body.newPassword)) {
		var n = request.body.newPassword;
	} else {
		return response.status(400).send("New password must only contain ASCII characters and must have a minimum length of 8 characters and a maximum length of 64 characters.\n");
	}

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

	// Validate password and newEmail
	if (validatePassword(request.body.password)) {
		var p = request.body.password;
	} else {
		return response.status(400).send("Invalid password.\n");
	}
	if (validateEmail(request.body.newEmail)) {
		var n = normalizeEmail(request.body.newEmail);
	} else {
		return response.status(400).send("New email must be valid and have a minimum length of 3 characters and a maximum length of 255 characters.\n");
	}

	changeEmail(p, n, request, response);
});

// Called when a POST request is made to /changeCrossRadius
app.post('/changeCrossRadius', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (newCrossRadius)
	if (Object.keys(request.body).length != 1 || !request.body.newCrossRadius) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate newCrossRadius
	if (validateCrossRadius(request.body.newCrossRadius)) {
		var n = request.body.newCrossRadius;
	} else {
		return response.status(400).send("Cross radius must be an integer with a minimum of 10 and a maximum of 5280.\n");
	}

	changeCrossRadius(n, request, response);
});

// Called when a POST request is made to /getCrossRadius
app.post('/getCrossRadius', function(request, response) {
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

	getCrossRadius(request, response);
});

// Called when a POST request is made to /forgotPassword
app.post('/forgotPassword', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 2 parameters (username and email)
	if (Object.keys(request.body).length != 2 || !request.body.username || !request.body.email) {
		return response.status(400).send("Invalid POST request\n");
	}

	// Validate username and email
	if (validateUsername(request.body.username)) {
		var u = request.body.username;
	} else {
		return response.status(400).send("Invalid username.\n");
	}
	if (validateEmail(request.body.email)) {
		var e = normalizeEmail(request.body.email);
	} else {
		return response.status(400).send("Invalid email.\n");
	}

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

	// POST request must have 2 parameters (inputNewPassword and inputConfirmPassword)
	if (Object.keys(request.body).length != 2 || !request.body.inputNewPassword || !request.body.inputConfirmPassword) {
		return response.status(400).send("Invalid POST request\n");
	}

	// POST request must have 1 query (token)
	if (Object.keys(request.query).length != 1 || !request.query.token) {
		return response.status(400).send("Invalid POST request\n");
	}

	// Validate token, inputNewPassword, and inputConfirmPassword
	if (validatePasswordResetToken(request.query.token)) {
		var token = request.query.token;
	} else {
		return response.status(400).send("Invalid password reset token.\n");
	}
	if (validatePassword(request.body.inputNewPassword) && validatePassword(request.body.inputConfirmPassword)) {
		var newPassword = request.body.inputNewPassword;
		var confirmPassword = request.body.inputConfirmPassword;
	} else {
		return response.status(400).send("New password must only contain ASCII characters and must have a minimum length of 8 characters and a maximum length of 64 characters.\n");
	}

	// Check that newPassword and confirmPassword are the same
	if (newPassword != confirmPassword) {
		return response.status(400).send("Passwords did not match.\n");
	}

	resetPassword(token, newPassword, confirmPassword, response);
});

// Called when a POST request is made to /addFirebaseRegistrationToken
app.post('/addFirebaseRegistrationToken', function(request, response) {
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (firebaseRegistrationToken)
	if (Object.keys(request.body).length != 1 || !request.body.firebaseRegistrationToken) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
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

	// POST request must have 5 parameters (email, firstname, lastname, loc, and about)
	if (Object.keys(request.body).length != 5 || !request.body.email || !request.body.firstName || !request.body.lastName || !request.body.loc || !request.body.about) {
		return response.status(400).send("Invalid POST request\n");
	}

	var e = request.body.email;
	var f = request.body.firstName;
	var l = request.body.lastName;
	var lo = request.body.loc;
	var a = request.body.about;

	updateLinkedInProfile(e, f, l, lo, a, response);
});

// Called when a POST request is made to /updateProfile
app.post('/updateProfile', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 5 parameters (name, loc, about, interests, and picture)
	if (Object.keys(request.body).length != 5 || !request.body.name || !request.body.loc || !request.body.about || !request.body.interests || !request.body.picture) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	var n = request.body.name;
	var l = request.body.loc;
	var a = request.body.about;
	var i = request.body.interests;
	var p = request.body.picture;

	updateProfile(n, l, a, i, p, request, response);
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
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
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

	updateLocation(lat, lon, request, response);
});

// Called when a POST request is made to /approveUser
app.post('/approveUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.\n");
	}

	approveUser(u, request, response);
});

// Called when a POST request is made to /unapproveUser
app.post('/unapproveUser', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.\n");
	}

	unapproveUser(u, request, response);
});

// Called when a POST request is made to /getLocationForHeatmap
app.post('/getLocationForHeatmap', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.send(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not autheticated
	if (!request.session || !request.session.authenticated || request.session.autheticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	getLocationForHeatmap(request, response);
});

// Called when a POST request is made to /getAllLocationsForHeatmap
app.post('/getAllLocationsForHeatmap', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.send(500);

	// POST request must have 0 parameters
	if (Object.keys(request.body).length != 0) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not autheticated
	if (!request.session || !request.session.authenticated || request.session.autheticated === false) {
		return response.status(400).send("User not logged in.\n");
	}

	getAllLocationsForHeatmap(request, response);
});

// Called when a POST request is made to /getAllMatches
app.post('/getAllMatches', function(request, response){
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

	getAllMatches(request, response);
});

// Called when a POST request is made to /getMatch
app.post('/getMatch', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.\n");
	}

	getMatch(u, request, response);
});

// Called when a POST request is made to /getCrossLocations
app.post('/getCrossLocations', function(request, response){
	// If the object request.body is null, respond with status 500 'Internal Server Error'
	if (!request.body) return response.sendStatus(500);

	// POST request must have 1 parameter (uid)
	if (Object.keys(request.body).length != 1 || !request.body.uid) {
		return response.status(400).send("Invalid POST request\n");
	}

	// If session not authenticated
	if (!request.session || ((!request.session.authenticated || request.session.authenticated === false) && (!request.session.googleAuthenticated || request.session.googleAuthenticated === false) && (!request.session.facebookAuthenticated || request.session.facebookAuthenticated === false))) {
		return response.status(400).send("User not logged in.\n");
	}

	// Validate uid
	if (validateUid(request.body.uid)) {
		var u = request.body.uid;
	} else {
		return response.status(400).send("Invalid user ID.\n");
	}

	getCrossLocations(u, request, response);
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

// Registers a user if username and email does not already exist
function register(u, p, e, response) {
	// Check if username or email already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=? OR ??=?";
	var post = [username, db_accounts, username, u, email, e];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 0) {
			return response.status(400).send("Username or email already exists! Try again.\n");
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
						return response.status(500).send("User ID collision!\n");
					} else {
						// Create account in accounts table
						var sql = "INSERT INTO ?? SET ?";
						var post = {uid: userID, username: u, password: hash, email: e, crossRadius: DEFAULT_CROSS_RADIUS};
						dbConnection.query(sql, [db_accounts, post], function(err, result) {
							if (err) throw err;
							// Create profile in profiles table
							var sql = "INSERT INTO ?? SET ??=?, ??=?";
							var post = [db_profiles, uid, userID, email, e];
							dbConnection.query(sql, post, function(err, result){
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

// Verifies user has an account and logs them in
function login(u, p, request, response) {
	// Get password hash and email for username
	var sql = "SELECT ??,??,?? FROM ?? WHERE ??=?";
	var post = [uid, password, email, db_accounts, username, u];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
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
					return response.status(200).send(JSON.stringify({"response":"pass"}));
				} else {
					return response.status(400).send("Invalid username or password. Try again.\n");
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
					return response.status(400).send("Email already exists!\n");
				} else {
					var userID = crypto.randomBytes(8).toString('hex');
					// Check if user ID already exists
					var sql = "SELECT ?? FROM ?? WHERE ??=?";
					var post = [uid, db_accounts, uid, userID];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.length != 0) {
							return response.status(500).send("User ID collision!\n");
						} else {
							// Create account in accounts table
							var sql = "INSERT INTO ?? SET ?";
							var post = {uid: userID, email: e, googleID: id, crossRadius: DEFAULT_CROSS_RADIUS};
							dbConnection.query(sql, [db_accounts, post], function(err, result) {
								if (err) throw err;
								// Create profile in profiles table
								var sql = "INSERT INTO ?? SET ??=?, ??=?";
								var post = [db_profiles, uid, userID, email, e];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
									// Send registration confirm email
									const msg = {
										to: e,
										from: 'support@vvander.me',
										subject: 'Welcome to Wander!',
										text: 'You have registered for a Wander account with your Google account. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + e,
										html: '<strong>You have registered for a Wander account with your Google account. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + e + '</strong>',
									};
									sgMail.send(msg);
									matchGraph.setNode(userID);
									writeMatchGraph();
									request.session.googleAuthenticated = true;
									request.session.uid = userID;
									request.session.email = e;
									console.log("Account created and logged in with Google.");
									return response.status(200).send(JSON.stringify({"response":"pass"}));
								});
							});
						}
					});
				}
			});
		} else if (result.length == 1) {
			if (result[0].email == e) {
				request.session.googleAuthenticated = true;
				request.session.uid = result[0].uid;
				request.session.email = result[0].email;
				console.log("User logged in with Google.");
				return response.status(200).send(JSON.stringify({"response":"pass"}));
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
						return response.status(200).send(JSON.stringify({"response":"pass"}));
					});
				});
			}
		} else {
			return response.status(500).send("Error with Google login.\n");
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
					return response.status(400).send("Email already exists!\n");
				} else {
					var userID = crypto.randomBytes(8).toString('hex');
					// Check if user ID already exists
					var sql = "SELECT ?? FROM ?? WHERE ??=?";
					var post = [uid, db_accounts, uid, userID];
					dbConnection.query(sql, post, function(err, result) {
						if (err) throw err;
						if (result.length != 0) {
							return response.status(500).send("User ID collision!\n");
						} else {
							// Create account in accounts table
							var sql = "INSERT INTO ?? SET ?";
							var post = {uid: userID, email: e, facebookID: id, crossRadius: DEFAULT_CROSS_RADIUS};
							dbConnection.query(sql, [db_accounts, post], function(err, result) {
								if (err) throw err;
								// Create profile in profiles table
								var sql = "INSERT INTO ?? SET ??=?, ??=?";
								var post = [db_profiles, uid, userID, email, e];
								dbConnection.query(sql, post, function(err, result){
									if (err) throw err;
									// Send registration confirm email
									const msg = {
										to: e,
										from: 'support@vvander.me',
										subject: 'Welcome to Wander!',
										text: 'You have registered for a Wander account with your Facebook account. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + e,
										html: '<strong>You have registered for a Wander account with your Facebook account. Click the following link to confirm your email: https://vvander.me/confirmEmail?email=' + e + '</strong>',
									};
									sgMail.send(msg);
									matchGraph.setNode(userID);
									writeMatchGraph();
									request.session.facebookAuthenticated = true;
									request.session.uid = userID;
									request.session.email = e;
									console.log("Account created and logged in with Facebook.");
									return response.status(200).send(JSON.stringify({"response":"pass"}));
								});
							});
						}
					});
				}
			});
		} else if (result.length == 1) {
			if (result[0].email == e) {
				request.session.facebookAuthenticated = true;
				request.session.uid = result[0].uid;
				request.session.email = result[0].email;
				console.log("User logged in with Facebook.");
				return response.status(200).send(JSON.stringify({"response":"pass"}));
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
						return response.status(200).send(JSON.stringify({"response":"pass"}));
					});
				});
			}
		} else {
			return response.status(500).send("Error with Facebook login.\n");
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
										from: 'support@vvander.me',
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

// Deletes an account created with Google
function googleDeleteAccount(request, response) {
	// Check that account exists
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(500).send("User ID not found.\n");
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
								from: 'support@vvander.me',
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
					return reponse.status(500).send("Error deleted multiple accounts.\n");
				} else if (result.affectedRows == 0) {
					return response.status(500).send("Failed to delete account.\n");
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
			return response.status(500).send("User ID not found.\n");
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
								from: 'support@vvander.me',
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
					return reponse.status(500).send("Error deleted multiple accounts.\n");
				} else if (result.affectedRows == 0) {
					return response.status(500).send("Failed to delete account.\n");
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
			return response.status(400).send("Username already exists! Try again.\n");
		} else {
			// Get password hash for user ID
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [password, db_accounts, uid, request.session.uid];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 1) {
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

// Changes the password of an account
function changePassword(p, n, request, response) {
	// Get password hash for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [password, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
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

// Changes the email of an account
function changeEmail(p, n, request, response) {
	// Check if new email already exists
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [email, db_accounts, email, n];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 0) {
			return response.status(400).send("Email already exists! Try again.\n");
		} else {
			// Get password hash for user ID
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [password, db_accounts, uid, request.session.uid];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length != 1) {
					return response.status(500).send("User ID not found.\n");
				} else {
					// Compare sent password hash to account password hash
					bcrypt.compare(p, result[0].password, function(err, res) {
						if (res !== true) {
							return response.status(400).send("Invalid password. Try again.\n");
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
											});
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
			return reponse.status(500).send("Error changed multiple account cross radii.\n");
		} else if (result.affectedRows == 0) {
			return response.status(500).send("Failed to change cross radius.\n");
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
			return response.status(500).send("Error getting cross radius.\n");
		} else {
			return response.status(200).send(JSON.stringify({"response":"pass", crossRadius:result[0].crossRadius}));
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

// Resets an account password
function resetPassword(token, newPassword, confirmPassword, response) {
	// Get email and passwordResetExpires for passwordResetToken
	var sql = "SELECT ??, ?? FROM ?? WHERE ??=?";
	var post = [email, passwordResetExpires, db_accounts, passwordResetToken, token];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
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
						return response.status(500).send("Firebase registration token already exists!\n");
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
				return response.status(500).send("Error updating Firebase registration token.\n");
			}
		});
	} else {
		// Check if Firebase registration token already exists
		var sql = "SELECT ?? FROM ?? WHERE ??=?";
		var post = [registrationToken, db_firebase, registrationToken, token];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			if (result.length != 0) {
				return response.status(500).send("Firebase registration token already exists!\n");
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
function updateLinkedInProfile(e, f, l, lo, a, response) {
	// Get profile for email
	var sql = "SELECT * FROM ?? WHERE ??=?";
	var post = [db_profiles, email, e];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		if (result.length != 1) {
			return response.status(400).send(JSON.stringify({"response":"fail"}));
		} else {
			// Update profile if already exists for email
			var sql = "UPDATE ?? SET ??=?, ??=?, ??=?, ??=? WHERE ??=?";
			var post = [db_profiles, firstName, f, lastName, l, loc, lo, about, a, email, e];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				console.log("LinkedIn profile updated."); 
				return response.status(200).send(JSON.stringify({"response":"pass"}));
			});
		}
	});
}

// Updates profile info
function updateProfile(n, l, a, i, p, request, response) {
	// Update profile for user ID
	var sql = "UPDATE ?? SET ??=?, ??=?, ??=?, ??=?, ??=? WHERE ??=?";
	var post = [db_profiles, firstName, n, loc, l, about, a, interests, i, picture, p, uid, request.session.uid];
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
			return response.status(400).send("No profile.\n");
		} else {
			var f = result[0].firstName;
			var l = result[0].lastName;
			var lo = result[0].loc;
			var a = result[0].about;
			var i = result[0].interests;
			var p = result[0].picture;
			return response.status(200).send(JSON.stringify({"response":"pass", firstName:f, lastName:l, loc:lo, about:a, interests:i, picture:p}));
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
		response.status(200).send(JSON.stringify({"response":"success"}));	
		findCrossedPaths(lat, lon, currentTime, request, response);
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

// Checks if anyone crossed paths
function findCrossedPaths(lat, lon, currentTime, request, response) {
	// Get cross radius for user ID
	var sql = "SELECT ?? FROM ?? WHERE ??=?";
	var post = [crossRadius, db_accounts, uid, request.session.uid];
	dbConnection.query(sql, post, function(err, result) {
		if (err) throw err;
		// Get all user IDs and coordinates within cross time and cross radius
		sql = "SELECT ??, ??, ?? FROM ?? WHERE ??!=? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ? AND ?? BETWEEN ? AND ?";
		var timeMin = currentTime - CROSS_TIME;
		var latMin = lat - feetToLat(result[0].crossRadius);
		var latMax = lat + feetToLat(result[0].crossRadius);
		var lonMin = lon - feetToLon(result[0].crossRadius, lat);
		var lonMax = lon + feetToLon(result[0].crossRadius, lat);
		post = [uid, latitude, longitude, db_locations, uid, request.session.uid, time, timeMin, currentTime, latitude, latMin, latMax, longitude, lonMin, lonMax];
		dbConnection.query(sql, post, function(err, result) {
			if (err) throw err;
			// For every result within cross time and cross radius
			for (var i = 0; i < result.length; i++) {
				var uidOther = result[i].uid;
				var latOther = result[i].latitude;
				var lonOther = result[i].longitude;
				// Get cross radius for other user ID
				sql = "SELECT ?? FROM ?? WHERE ??=?";
				post = [crossRadius, db_accounts, uid, uidOther];
				dbConnection.query(sql, post, function(err, result) {
					if (err) throw err;
					var otherLatMin = latOther - feetToLat(result[0].crossRadius);
					var otherLatMax = latOther + feetToLat(result[0].crossRadius);
					var otherLonMin = lonOther - feetToLon(result[0].crossRadius, latOther);
					var otherLonMax = lonOther + feetToLon(result[0].crossRadius, latOther);
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
							if (matchGraph.edge(request.session.uid, uidOther, "timesCrossed") >= MATCH_THRESHOLD && matchGraph.edge(uidOther, request.session.uid, "timesCrossed") >= MATCH_THRESHOLD && !matchGraph.hasEdge(request.session.uid, uidOther, "matched") && !matchGraph.hasEdge(uidOther, request.session.uid, "matched")) {
								// If crossed greater than or equal to match threshold times and not already matched, create matched, approved, unmatched, blocked, and newMatch edges
								matchGraph.setEdge(request.session.uid, uidOther, true, "newMatch");
								matchGraph.setEdge(uidOther, request.session.uid, true, "newMatch");
								console.log('Users matched.');
							} else if (matchGraph.edge(request.session.uid, uidOther, "timesCrossed") >= MATCH_THRESHOLD && matchGraph.edge(uidOther, request.session.uid, "timesCrossed") >= MATCH_THRESHOLD && matchGraph.hasEdge(request.session.uid, uidOther, "matched") && matchGraph.hasEdge(uidOther, request.session.uid, "matched")) {
								// If crossed and already matched, notify users
								var sql = "SELECT ?? FROM ?? WHERE ??=?";
								var post = [registrationToken, db_firebase, uid, request.session.uid];
								dbConnection.query(sql, post, function(err, result) {
									if (err) throw err;
									if (result.length > 0) {
										for (var i = 0; i < result.length; i++) {
											var message = {
												data: {
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
													console.log('Successfully sent existing match crossed paths notification.');
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
													console.log('Successfully sent existing match crossed paths notification.');
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
			writeMatchGraph();
		});
	});
}

// Notifies users who have matched
function notifyMatches() {
	// Notify all users that have new matches, then remove newMatch edges
	var edges = matchGraph.edges();
	var edgesToRemove = [];
	for (var i = 0; i < matchGraph.edgeCount(); i++) {
		if (edges[i] != null && edges[i].name === "newMatch") {
			// Create matched, approved, unmatched, and blocked edges
			matchGraph.setEdge(edges[i].v, edges[i].w, true, "matched");
			matchGraph.setEdge(edges[i].w, edges[i].v, true, "matched");
			matchGraph.setEdge(edges[i].v, edges[i].w, false, "approved");
			matchGraph.setEdge(edges[i].w, edges[i].v, false, "approved");
			matchGraph.setEdge(edges[i].v, edges[i].w, false, "unmatched");
			matchGraph.setEdge(edges[i].w, edges[i].v, false, "unmatched");
			matchGraph.setEdge(edges[i].v, edges[i].w, false, "blocked");
			matchGraph.setEdge(edges[i].w, edges[i].v, false, "blocked");
			// Notify user ID edges[i].v of match with user ID edges[i].w
			var sql = "SELECT ?? FROM ?? WHERE ??=?";
			var post = [registrationToken, db_firebase, uid, edges[i].v];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length > 0) {
					for (var j = 0; j < result.length; j++) {
						var message = {
							data: {
								title: 'You have a new match!',
								body: 'Tap to see who you matched with.',
								uid: edges[i].w
							},
							token: result[j].registrationToken,
							android: {
								ttl: 3600000,
								priority: 'high',
							}
						};
						admin.messaging().send(message)
							.then((response) => {
								console.log('Successfully sent match notification.');
							})
							.catch((error) => {
								console.log(error);
							});
					}
				}
			});
			edgesToRemove.push([edges[i].v, edges[i].w]);
		}
	}
	for (var i = 0; i < edgesToRemove.length; i++) {
		matchGraph.removeEdge(edgesToRemove[i][0], edgesToRemove[i][1], "newMatch");
	}
	writeMatchGraph();
	console.log('Matches notified.');
}

// Approves the user with the given user ID
function approveUser(u, request, response) {
	matchGraph.setEdge(request.session.uid, u, true, "approved");
	writeMatchGraph();
	console.log('User approved.');
	return response.status(200).send(JSON.stringify({"response":"success"}));	
	
}

// Unapproves the user with the given user ID
function unapproveUser(u, request, response) {
	matchGraph.setEdge(request.session.uid, u, false, "approved");
	writeMatchGraph();
	console.log('User unapproved.');
	return response.status(200).send(JSON.stringify({"response":"success"}));	
}

// Gets all location coordinates for user ID for heatmap generation
function getLocationForHeatmap(request, response) {
	var sql = "SELECT ??,?? FROM ?? WHERE ??=?";
	var post = [longitude, latitude, db_locations, uid, request.session.uid];
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
		console.log("User location for heatmap sent.");
		return response.status(200).send(JSON.stringify(object));
	});
}

// Gets all location coordinates for all users for heatmap generation
function getAllLocationsForHeatmap(request, response) {
	var sql = "SELECT ??,?? FROM ??";
	var post = [longitude, latitude, db_locations];
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
		console.log(edges[i].name);
		if (edges[i].name === "matched") {
			var data = {uid: edges[i].w};
			object[key].push(data);
		}
	}
	console.log("Sent all matches.");
	return response.status(200).send(JSON.stringify(object));
}

// Gets information of a single match
function getMatch(u, request, response) {
	var edges = matchGraph.outEdges(request.session.uid, u);
	for (var i = 0; i < edges.length; i++) {
		if (edges[i].name === "matched") {
			var sql = "SELECT * FROM ?? WHERE ??=?";
			var post = [db_profiles, uid, edges[i].w];
			dbConnection.query(sql, post, function(err, result) {
				if (err) throw err;
				if (result.length == 0) {
					return response.status(500).send("Error getting match information.\n");
				} else {
					var object = {};
					var key = "Profile";
					object[key] = [];
					for (var j = 0; j < result.length; j++) {
						var n = result[j].firstName;
						var a = result[j].about;
						var i = result[j].interests;
						var p = result[j].picture;
						var t = matchGraph.edge(request.session.uid, result[j].uid, "timesCrossed");
						var ap = matchGraph.edge(request.session.uid, result[j].uid, "approved");
						//console.log(ap);
						var oap = matchGraph.edge(result[j].uid, request.session.uid, "approved");
						//console.log(oap);
						var data = {uid: result[j].uid, firstName: n, about: a, interests: i, picture: p, timesCrossed: t, approved: ap, otherApproved: oap};
						object[key].push(data);
					}
					return response.status(200).send(JSON.stringify(object));
				}
			});
		}
	}
}

// Gets all location coordinates where users crossed paths
function getCrossLocations(u, request, response) {
	if (matchGraph.hasEdge(request.session.uid, u, "crossLocations") && matchGraph.hasEdge(u, request.session.uid, "crossLocations")) {
		console.log("Cross locations sent.");
		return response.status(200).send(matchGraph.edge(request.session.uid, u, "crossLocations"));
	}
	return response.status(500).send("Error getting cross locations.\n");
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
