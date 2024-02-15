var express = require('express')
var fs = require('fs')

var router = express.Router()

var configPath = process.env.CONFIG_PATH

const appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

router.get('/', function(req, res, next) {
        res.render('health', {
            content: JSON.stringify({ "api_url": appConfig.client_api_url, "web_url": appConfig.client_web_url }),
            layout: 'health'
        })
})

module.exports = router
