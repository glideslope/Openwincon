module.exports = {
    getApPage: (req, res) => {
        let query = "SELECT * FROM `nas` ORDER BY id ASC"; // query database to get all the players

        // execute query
        db.query(query, (err, result) => {
            if (err) {
                res.redirect('/');
            }
            res.render('apindex.ejs', {
                title: "WiFi Authentication Manager | View APs"
                ,aps: result
            });
        });
    },
};


