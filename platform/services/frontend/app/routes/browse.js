var express = require('express')
var https = require('https')
var axios = require('axios')
var fs = require('fs')
var grid = require('./grid')

var router = express.Router()

var configPath = process.env.CONFIG_PATH

var secretsPath = process.env.SECRETS_PATH

const agent = new https.Agent({
    rejectUnauthorized: false,
    ca: fs.readFileSync(secretsPath + '/ca_cert.pem')
})

const appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

extractToken = (req) => {
    let authorization = req.headers.authorization
    if (authorization == undefined || !authorization.startsWith("Bearer ")) {
        authorization = req.cookies.token
    }
    return authorization
}

loadAccount = (config, req, callback) => {
    axios.get(appConfig.server_api_url + '/v1/accounts/me', config)
        .then(function (response) {
            req.resume()
            if (response.status == 200) {
                if (req.app.get('env') === 'development') {
                    console.log("account = " + JSON.stringify(response.data))
                }
                callback(response.data)
            } else {
                console.log("Can't load account: status = " + content.status)
                callback({})
            }
        })
        .catch(function (error) {
            req.resume()
            console.log("Can't load account " + error)
            callback({})
        })
}

router.get('/designs.html', function(req, res, next) {
    authorization = extractToken(req)

    let config = {
        timeout: 10000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': 'Bearer ' + authorization }
    }

    req.pause()

    loadAccount(config, req, (account) => {
        axios.get(appConfig.server_api_url + '/v1/designs', config)
            .then(function (response) {
                req.resume()
                if (response.status == 200) {
                    if (req.app.get('env') === 'development') {
                        console.log("designs = " + JSON.stringify(response.data))
                    }
                    console.log(account)
                    let designs = response.data.map((design) => ({
                        uuid: design.uuid,
                        checksum: design.checksum,
                        location: appConfig.client_web_url + '/browse/designs/' + design.uuid + ".html",
                        imageUrl: appConfig.client_web_url + '/browse/designs/' + design.uuid + "/0/0/0/256.png",
                        baseUrl: appConfig.client_web_url + '/browse/designs',
                        modified: design.modified
                    }))
                    res.render('browse/designs', {
                        config: {"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url},
                        layout: 'browse',
                        title: 'Designs',
                        url: appConfig.client_web_url,
                        login: account.role == null,
                        admin: account.role === 'admin',
                        grid: grid.make(designs)
                    })
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    res.render('browse/designs', {
                        config: {"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url},
                        layout: 'browse',
                        title: 'Designs',
                        url: appConfig.client_web_url,
                        login: account.role == null,
                        admin: account.role === 'admin',
                        grid: []
                    })
                }
            })
            .catch(function (error) {
                req.resume()
                console.log("Can't load designs " + error)
                res.render('browse/designs', {
                    config: {"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url},
                    layout: 'browse',
                    title: 'Designs',
                    url: appConfig.client_web_url,
                    login: account.role == null,
                    admin: account.role === 'admin',
                    grid: []
                })
            })
    })
})

router.get('/designs/(:uuid).html', function(req, res, next) {
    authorization = extractToken(req)

    let config = {
        timeout: 10000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': 'Bearer ' + authorization }
    }

    req.pause()

    loadAccount(config, req, (account) => {
        axios.get(appConfig.server_api_url + '/v1/designs/' + req.params.uuid, config)
            .then(function (response) {
                req.resume()
                if (response.status == 200) {
                    if (req.app.get('env') === 'development') {
                        console.log("design = " + JSON.stringify(response.data))
                    }
                    let design = response.data
                    design = {
                        uuid: design.uuid,
                        checksum: design.checksum,
                        location: appConfig.client_web_url + '/browse/designs/' + design.uuid + ".html",
                        imageUrl: appConfig.client_web_url + '/browse/designs/' + design.uuid + "/0/0/0/512.png",
                        baseUrl: appConfig.client_web_url + '/browse/designs',
                        modified: design.modified
                    }
                    res.render('browse/preview', {
                        config: {"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url},
                        layout: 'browse',
                        title: 'Designs | ' + req.params.uuid,
                        url: appConfig.client_web_url,
                        uuid: req.params.uuid,
                        login: account.role == null,
                        admin: account.role === 'admin',
                        design: design
                    })
                } else {
                    console.log("Can't load design: status = " + content.status)
                    res.render('browse/preview', {
                        config: {"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url},
                        layout: 'browse',
                        title: 'Designs | ' + req.params.uuid,
                        url: appConfig.client_web_url,
                        uuid: req.params.uuid,
                        login: account.role == null,
                        admin: account.role === 'admin'
                    })
                }
            })
            .catch(function (error) {
                req.resume()
                console.log("Can't load design " + error)
                res.render('browse/preview', {
                    config: {"api_url":appConfig.client_api_url,"web_url":appConfig.client_web_url},
                    layout: 'browse',
                    title: 'Designs | ' + req.params.uuid,
                    url: appConfig.client_web_url,
                    uuid: req.params.uuid,
                    login: account.role == null,
                    admin: account.role === 'admin'
                })
            })
    })
})

router.get('/designs/(:uuid)/(:zoom)/(:x)/(:y)/(:size).png', function(req, res, next) {
    authorization = extractToken(req)

    let config = {
        timeout: 10000,
        responseType:'stream',
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': 'Bearer ' + authorization }
    }

    const path = req.params.uuid + '/' + req.params.zoom + '/' + req.params.x + '/' + req.params.y + '/' + req.params.size + '.png'

    req.pause()

    axios.get(appConfig.server_api_url + '/v1/designs/' + path, config)
        .then(function (response) {
            req.resume()
            if (response.status == 200) {
                res.set('Cache-Control', 'public, max-age=3600, immutable')
                response.data.pipe(res, {end:true})
            } else {
                console.log("Can't load image " + error)
                res.status(500).end()
            }
        })
        .catch(function (error) {
            req.resume()
            console.log("Can't load image " + error)
            res.status(500).end()
        })
})

module.exports = router
