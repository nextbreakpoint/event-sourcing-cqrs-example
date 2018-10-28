var express = require('express');
var fs = require('fs');
var router = express.Router();

var configPath = process.env.CONFIG_PATH || '../../../environments/development';

const appConfig = JSON.parse(fs.readFileSync(configPath + '/config.json', 'utf8'));

router.get('/designs.html', function(req, res, next) {
    res.render('admin/designs', {
        config: JSON.stringify(appConfig),
        layout: 'layout',
        title: 'Designs',
        url: appConfig.web_url
    });
});

router.get('/designs/(:uuid).html', function(req, res, next) {
    res.render('admin/preview', {
        config: JSON.stringify(appConfig),
        layout: 'layout',
        title: 'Designs | ' + req.params.uuid,
        url: appConfig.web_url, uuid: req.params.uuid
    });
});

module.exports = router;
