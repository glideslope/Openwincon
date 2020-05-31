const fs = require('fs');

module.exports = {
    addApPage: (req, res) => {
        res.render('add-ap.ejs', {
            title: "WiFi Authentication Manager | Add a new AP"
            ,message: ''
        });
    },
    addAp: (req, res) => {
//        if (!req.username) {
//            return res.status(400).send("No username is given.");
//        }

        let message = '';
        let ipaddress = req.body.ipaddress;
        let apname = req.body.apname;
        let ports = req.body.ports;
	let secret = req.body.secret;

//	console.log('input: ports= '+ports);
        let ApQuery = "SELECT * FROM nas WHERE nasname = '" + ipaddress + "'";

        db.query(ApQuery, (err, result) => {
            if (err) {
                return res.status(500).send(err);
            }
            if (result.length > 0) {
                message = 'IP address already exists';
                res.render('add-ap.ejs', {
                    message,
                    title: "WiFi Authentication Manager | Add a new AP"
                });
            } else {
                // send the player's details to the database
		if (ports != "") {
                    let query = "INSERT INTO nas (nasname, shortname, ports, secret) VALUES ('" +
                    ipaddress + "', '" + apname + "', '" + ports + "', '" + secret + "')";
                    db.query(query, (err, result) => {
                        if (err) {
                            return res.status(500).send(err);
                        }
                        res.redirect('/ap');
		
                    });
		} else {
                    let query = "INSERT INTO nas (nasname, shortname, secret) VALUES ('" +
                    ipaddress + "', '" + apname + "', '" + secret + "')";
                    db.query(query, (err, result) => {
                        if (err) {
                            return res.status(500).send(err);
                        }
                        res.redirect('/ap');
		
                    });
		}
            }
        });
    },
    editApPage: (req, res) => {
        let apId = req.params.id;
        let query = "SELECT * FROM nas WHERE id = '" + apId + "' ";
        db.query(query, (err, result) => {
            if (err) {
                return res.status(500).send(err);
            }
            res.render('edit-ap.ejs', {
                title: "Edit  AP"
                ,ap: result[0]
                ,message: ''
            });
        });
    },
    editAp: (req, res) => {
        let apId = req.params.id;
	let ipaddress = req.body.ipaddress;
        let apname = req.body.apname;
        let ports = req.body.ports;
	let secret = req.body.secret;

	if (ports != "") {
	        let query = "UPDATE nas SET nasname = '" + ipaddress + "', shortname = '" + apname + "', ports = '" + ports + "', secret = '" + secret + "' WHERE id = " + apId;
                db.query(query, (err, result) => {
                    if (err) {
                        return res.status(500).send(err);
                    }
                    res.redirect('/ap');
		});
	} else {
	        let query = "UPDATE nas SET nasname = '" + ipaddress + "', shortname = '" + apname + "', secret = '" + secret + "' WHERE id = " + apId;

                db.query(query, (err, result) => {
                    if (err) {
                        return res.status(500).send(err);
                    }
                    res.redirect('/ap');
		
                });
	}
    },
    deleteAp: (req, res) => {
        let apId = req.params.id;
        let deleteUserQuery = 'DELETE FROM nas WHERE id = "' + apId + '"';
        db.query(deleteUserQuery, (err, result) => {
           if (err) {
                return res.status(500).send(err);
           }
           res.redirect('/ap');
        });
    }
};

