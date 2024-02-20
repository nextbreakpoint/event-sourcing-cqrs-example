let express = require('express')
let fs = require('fs')

let router = express.Router()

let configPath = process.env.CONFIG_PATH

let appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

let config = {
    "api_url": appConfig.client_api_url,
    "web_url": appConfig.client_web_url
}

router.get('/designs.html', function(req, res, next) {
    res.render('admin/designs', {
        config: JSON.stringify(config),
        layout: 'admin',
        title: 'Designs',
        url: appConfig.client_web_url
    })
})

router.get('/designs/(:uuid).html', function(req, res, next) {
    res.render('admin/preview', {
        config: JSON.stringify(config),
        layout: 'admin',
        title: 'Designs | ' + req.params.uuid,
        url: appConfig.client_web_url,
        uuid: req.params.uuid
    })
})

module.exports = router
