module.exports = {
    getUserPage: (req, res) => {
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
};


