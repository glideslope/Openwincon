module.exports = {
    getHomePage: (req, res) => {
	    res.render('index.ejs', {
		    title: "WiFi Authentication Manager | Select modes"
	    });
    },

/*
        let query = "SELECT * FROM `radcheck` ORDER BY id ASC"; // query database to get all the players

        // execute query
        db.query(query, (err, result) => {
            if (err) {
                res.redirect('/');
            }
            res.render('userindex.ejs', {
                title: "WiFi Authentication Manager | View Users"
                ,users: result
            });
        });
    },
 */
};


