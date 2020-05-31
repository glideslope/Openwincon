var express = require('express');
var mysql = require('mysql');
var dbconfig = require('./config/database.js');
var connection = mysql.createConnection(dbconfig);

var app = express();

// configuration
app.set('port', process.env.PORT || 3001);

app.get('/', function(req,res){
	res.send('Root');
});

app.get('/persons', function(req,res){

	connection.query('SELECT * from radcheck', function(err, rows) {
		if(err) throw err;

		console.log('users: ', rows);
		res.send(rows);
	});
});

app.listen(app.get('port'), function () {
	console.log('Express server listening on port ' + app.get('port'));
});

