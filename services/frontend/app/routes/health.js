let express = require('express')
let fs = require('fs')

let router = express.Router()

let configPath = process.env.CONFIG_PATH

let appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

let config = {
    "api_url": appConfig.client_api_url,
    "web_url": appConfig.client_web_url
}

router.get('/', function(req, res, next) {
    res.json(config)
})

module.exports = router
