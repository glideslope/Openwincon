const fs = require('fs');

module.exports = {
    addUserPage: (req, res) => {
        res.render('add-user.ejs', {
            title: "WiFi Authentication Manager | Add a new user"
            ,message: ''
        });
    },
    addUser: (req, res) => {
//        if (!req.username) {
//            return res.status(400).send("No username is given.");
//        }

        let message = '';
        let username = req.body.username;
        let attribute = req.body.attribute;
        let value = req.body.value;

//	console.log('input: name='+username+req.body.username+req.body);
        let usernameQuery = "SELECT * FROM radcheck WHERE username = '" + username + "'";

        db.query(usernameQuery, (err, result) => {
            if (err) {
                return res.status(500).send(err);
            }
            if (result.length > 0) {
                message = 'Username already exists';
                res.render('add-user.ejs', {
                    message,
                    title: "WiFi Authentication Manager | Add a new user"
                });
            } else {
                // send the player's details to the database
                let query = "INSERT INTO radcheck (username, attribute, op, value) VALUES ('" +
                username + "', '" + attribute + "', ':=', '" + value + "')";
                db.query(query, (err, result) => {
                    if (err) {
                        return res.status(500).send(err);
                    }
                    res.redirect('/user');
                });
            }
        });
    },
    editUserPage: (req, res) => {
        let userId = req.params.id;
        let query = "SELECT * FROM radcheck WHERE id = '" + userId + "' ";
        db.query(query, (err, result) => {
            if (err) {
                return res.status(500).send(err);
            }
            res.render('edit-user.ejs', {
                title: "Edit  User"
                ,user: result[0]
                ,message: ''
            });
        });
    },
    editUser: (req, res) => {
        let userId = req.params.id;
	let username = req.body.username;
        let passtype = req.body.passtype;
        let password = req.body.password;

        let query = "UPDATE radcheck SET username = '" + username + "', attribute = '" + passtype + "', value = '" + password + "'" + " WHERE id = " + userId;
        db.query(query, (err, result) => {
            if (err) {
                return res.status(500).send(err);
            }
            res.redirect('/user');
        });
    },
    deleteUser: (req, res) => {
        let userId = req.params.id;
        let deleteUserQuery = 'DELETE FROM radcheck WHERE id = "' + userId + '"';
        db.query(deleteUserQuery, (err, result) => {
           if (err) {
                return res.status(500).send(err);
           }
           res.redirect('/user');
        });
    }
};

