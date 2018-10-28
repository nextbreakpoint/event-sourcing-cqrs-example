var express = require('express');
var router = express.Router();

var configPath = process.env.CONFIG_PATH || '../../../../environments/development';

router.get('/config.json', function(req, res, next) {
    var options = {
        root: __dirname,
        cacheControl: false,
        maxAge: 600000
    };
    res.sendFile(configPath + '/config.json', options);
});

module.exports = router;
