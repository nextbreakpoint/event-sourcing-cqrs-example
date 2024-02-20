let express = require('express')
let https = require('https')
let axios = require('axios')
let fs = require('fs')
let grid = require('./grid')

let router = express.Router()

let configPath = process.env.CONFIG_PATH

let secretsPath = process.env.SECRETS_PATH

let agent = new https.Agent({
//    rejectUnauthorized: false
//    ca: fs.readFileSync(secretsPath + '/ca_cert.pem')
})

let appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

let config = {
    "api_url": appConfig.client_api_url,
    "web_url": appConfig.client_web_url
}

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
        timeout: 30000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': 'Bearer ' + authorization }
    }

    req.pause()

    let page = req.query.page ? new Number(req.query.page) : 0
    let scroll = req.query.scroll ? new Number(req.query.scroll) : 0

    let size = 10
    let from = page * size

    loadAccount(config, req, (account) => {
        if (account !== {}) {
            console.log(account)
        }
        axios.get(appConfig.server_api_url + '/v1/designs?from=' + from + "&size=" + size, config)
            .then(function (response) {
                req.resume()
                if (response.status == 200) {
                    if (req.app.get('env') === 'development') {
//                        console.log("designs = " + JSON.stringify(response.data))
                        console.log("designs = " + response.data.total)
                    }
                    let designs = response.data.designs.map((design) => ({
                        uuid: design.uuid,
                        checksum: design.checksum,
                        location: appConfig.client_web_url + '/browse/designs/' + design.uuid + ".html",
                        imageUrl: appConfig.client_web_url + '/browse/designs/' + design.uuid + "/0/0/0/256.png?t=" + design.checksum,
                        baseUrl: appConfig.client_web_url + '/browse/designs',
                        modified: design.modified
                    }))
                    res.render('browse/designs', {
                        config: config,
                        layout: 'browse',
                        title: 'Designs',
                        url: appConfig.client_web_url,
                        login: account.role == null,
                        logout: account.role != null,
                        admin: account.role === 'admin',
                        grid: grid.make(designs, from, size),
                        show: response.data.total > from + size,
                        page: page,
                        lastPage: Math.ceil(response.data.total / size) - 1,
                        scroll: scroll
                    })
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    res.render('browse/designs', {
                        config: config,
                        layout: 'browse',
                        title: 'Designs',
                        url: appConfig.client_web_url,
                        login: account.role == null,
                        logout: account.role != null,
                        admin: account.role === 'admin',
                        grid: [],
                        show: false,
                        page: page,
                        lastPage: 0,
                        scroll: 0
                    })
                }
            })
            .catch(function (error) {
                req.resume()
                console.log("Can't load designs " + error)
                res.render('browse/designs', {
                    config: config,
                    layout: 'browse',
                    title: 'Designs',
                    url: appConfig.client_web_url,
                    login: account.role == null,
                    logout: account.role != null,
                    admin: account.role === 'admin',
                    grid: [],
                    show: false,
                    page: 0,
                    lastPage: 0,
                    scroll: 0
                })
            })
    })
})

router.get('/designs.json', function(req, res, next) {
    authorization = extractToken(req)

    let config = {
        timeout: 30000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': 'Bearer ' + authorization }
    }

    req.pause()

    let page = req.query.page ? new Number(req.query.page) : 0
    let scroll = req.query.scroll ? new Number(req.query.scroll) : 0

    let size = 10
    let from = page * size

    loadAccount(config, req, (account) => {
        if (account !== {}) {
            console.log(account)
        }
        axios.get(appConfig.server_api_url + '/v1/designs?from=' + from + "&size=" + size, config)
            .then(function (response) {
                req.resume()
                if (response.status == 200) {
                    if (req.app.get('env') === 'development') {
//                        console.log("designs = " + JSON.stringify(response.data))
                        console.log("designs = " + response.data.total)
                    }
                    let designs = response.data.designs.map((design) => ({
                        uuid: design.uuid,
                        checksum: design.checksum,
                        location: appConfig.client_web_url + '/browse/designs/' + design.uuid + ".html",
                        imageUrl: appConfig.client_web_url + '/browse/designs/' + design.uuid + "/0/0/0/256.png?t=" + design.checksum,
                        baseUrl: appConfig.client_web_url + '/browse/designs',
                        modified: design.modified
                    }))
                    res.send(grid.make(designs, from, size))
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    res.send(grid.make([], from, size))
                }
            })
            .catch(function (error) {
                req.resume()
                console.log("Can't load designs " + error)
                res.send(grid.make([], from, size))
            })
    })
})

router.get('/designs/(:uuid).html', function(req, res, next) {
    authorization = extractToken(req)

    let config = {
        timeout: 30000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': 'Bearer ' + authorization }
    }

    req.pause()

    let page = req.query.page ? new Number(req.query.page) : 0
    let scroll = req.query.scroll ? new Number(req.query.scroll) : 0

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
                        location: '../browse/designs/' + design.uuid + ".html",
                        imageUrl: appConfig.client_web_url + '/browse/designs/' + design.uuid + "/0/0/0/512.png?t=" + design.checksum,
                        baseUrl: appConfig.client_web_url + '/browse/designs',
                        modified: design.modified
                    }
                    res.render('browse/preview', {
                        config: config,
                        layout: 'browse',
                        title: 'Designs | ' + req.params.uuid,
                        url: appConfig.client_web_url,
                        uuid: req.params.uuid,
                        login: account.role == null,
                        logout: account.role != null,
                        admin: account.role === 'admin',
                        design: design,
                        page: page,
                        scroll: scroll
                    })
                } else {
                    console.log("Can't load design: status = " + content.status)
                    res.render('browse/preview', {
                        config: config,
                        layout: 'browse',
                        title: 'Designs | ' + req.params.uuid,
                        url: appConfig.client_web_url,
                        uuid: req.params.uuid,
                        login: account.role == null,
                        logout: account.role != null,
                        admin: account.role === 'admin',
                        page: 0,
                        scroll: 0
                    })
                }
            })
            .catch(function (error) {
                req.resume()
                console.log("Can't load design " + error)
                res.render('browse/preview', {
                    config: config,
                    layout: 'browse',
                    title: 'Designs | ' + req.params.uuid,
                    url: appConfig.client_web_url,
                    uuid: req.params.uuid,
                    login: account.role == null,
                    logout: account.role != null,
                    admin: account.role === 'admin',
                    page: 0,
                    scroll: 0
                })
            })
    })
})

router.get('/designs/(:uuid)/(:zoom)/(:x)/(:y)/(:size).png', function(req, res, next) {
    authorization = extractToken(req)

    let config = {
        timeout: 60000,
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
