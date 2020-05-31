const express = require('express');
const bodyParser = require('body-parser');
const path = require('path');
const app = express();

const {getHomePage} = require('./routes/index');
const {getUserPage} = require('./routes/userindex');
const {addUserPage, addUser, deleteUser, editUser, editUserPage} = require('./routes/user');
const {getApPage} = require('./routes/apindex');
const {addApPage, addAp, deleteAp, editAp, editApPage} = require('./routes/ap');

const port = 3001;

// mysql
const mysql = require('mysql');
const dbconfig = require('./config/database.js');
const db = mysql.createConnection(dbconfig);

// connect to database
db.connect((err) => {
    if (err) {
        throw err;
    }
    console.log('Connected to database');
});
global.db = db;

app.set('port', process.env.port || port); // set express to use this port
app.set('views', __dirname + '/views'); // set express to look in this folder to render our view
app.set('view engine', 'ejs'); // configure template engine
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json()); // parse form data client
app.use(express.static('./public'));
//app.use(express.static(path.join(__dirname, 'public'))); // configure express to use public folder

// routes
app.get('/', getHomePage);
app.get('/user', getUserPage);
app.get('/user/add', addUserPage);
app.get('/user/edit/:id', editUserPage);
app.get('/user/delete/:id', deleteUser);
app.post('/user/add', addUser);
app.post('/user/edit/:id', editUser);

app.get('/ap', getApPage);
app.get('/ap/add', addApPage);
app.get('/ap/edit/:id', editApPage);
app.get('/ap/delete/:id', deleteAp);
app.post('/ap/add', addAp);
app.post('/ap/edit/:id', editAp);



// set the app to listen on the port
app.listen(port, () => {
    console.log(`Server running on port: ${port}`);
});

