var express = require('express')
var fs = require('fs')

var router = express.Router()

var configPath = process.env.CONFIG_PATH

const appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

router.get('/designs.html', function(req, res, next) {
    res.render('admin/designs', {
        config: JSON.stringify({"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url}),
        layout: 'admin',
        title: 'Designs',
        url: appConfig.client_web_url
    })
})

router.get('/designs/(:uuid).html', function(req, res, next) {
    res.render('admin/preview', {
        config: JSON.stringify({"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url}),
        layout: 'admin',
        title: 'Designs | ' + req.params.uuid,
        url: appConfig.client_web_url, uuid: req.params.uuid
    })
})

module.exports = router
