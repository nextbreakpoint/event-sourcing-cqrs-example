var express = require('express');
var axios = require('axios');
var fs = require('fs');
var https = require('https');
var router = express.Router();

var configPath = process.env.CONFIG_PATH || '../../../environments/development';

var secretsPath = process.env.SECRETS_PATH || '../../../../secrets';

const agent = new https.Agent({
    rejectUnauthorized: false,
    ca: fs.readFileSync(secretsPath + '/ca_cert.pem')
});

const appConfig = JSON.parse(fs.readFileSync(configPath + '/config.json', 'utf8'));

extractToken = (req) => {
    let authorization = req.headers.authorization;
    if (authorization == undefined || !authorization.startsWith("Bearer ")) {
        authorization = req.cookies.token;
    }
    return authorization;
}

router.get('/designs.html', function(req, res, next) {
    authorization = extractToken(req);

    let config = {
        timeout: 10000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': authorization }
    }

    req.pause();

    axios.get(appConfig.api_url + '/designs', config)
        .then(function (response) {
            req.resume();
            if (response.status == 200) {
                if (req.app.get('env') === 'development') {
                    console.log(JSON.stringify(response.data));
                }
                let designs = response.data.map((design) => ({
                    uuid: design.uuid,
                    checksum: design.checksum,
                    location: appConfig.web_url + '/content/designs/' + design.uuid + ".html",
                    imageUrl: appConfig.web_url + '/content/designs/' + design.uuid + "/0/0/0/256.png",
                    modified: design.modified
                }))
                res.render('content/designs', {
                    config: appConfig,
                    layout: 'bootstrap',
                    title: 'Designs',
                    url: appConfig.web_url,
                    designs: designs
                });
            } else {
                console.log("Can't load designs: status = " + content.status)
                res.render('content/designs', {
                    config: appConfig,
                    layout: 'bootstrap',
                    title: 'Designs',
                    url: appConfig.web_url,
                    designs: []
                });
            }
        })
        .catch(function (error) {
            req.resume();
            console.log("Can't load designs " + error)
            res.render('content/designs', {
                config: appConfig,
                layout: 'bootstrap',
                title: 'Designs',
                url: appConfig.web_url,
                designs: []
            });
        })
});

router.get('/designs/(:uuid).html', function(req, res, next) {
    authorization = extractToken(req);

    let config = {
        timeout: 10000,
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': authorization }
    }

    req.pause();

    axios.get(appConfig.api_url + '/designs/' + req.params.uuid, config)
        .then(function (response) {
            req.resume();
            if (response.status == 200) {
                if (req.app.get('env') === 'development') {
                    console.log(JSON.stringify(response.data));
                }
                let design = response.data;
                design = {
                    uuid: design.uuid,
                    checksum: design.checksum,
                    location: appConfig.web_url + '/content/designs/' + design.uuid + ".html",
                    imageUrl: appConfig.web_url + '/content/designs/' + design.uuid + "/0/0/0/512.png",
                    modified: design.modified
                };
                res.render('content/preview', {
                    config: appConfig,
                    layout: 'bootstrap',
                    title: 'Designs | ' + req.params.uuid,
                    url: appConfig.web_url,
                    uuid: req.params.uuid,
                    design: design
                });
            } else {
                console.log("Can't load design: status = " + content.status)
                res.render('content/preview', {
                    config: appConfig,
                    layout: 'bootstrap',
                    title: 'Designs | ' + req.params.uuid,
                    url: appConfig.web_url,
                    uuid: req.params.uuid
                });
            }
        })
        .catch(function (error) {
            req.resume();
            console.log("Can't load design " + error)
            res.render('content/preview', {
                config: appConfig,
                layout: 'bootstrap',
                title: 'Designs | ' + req.params.uuid,
                url: appConfig.web_url,
                uuid: req.params.uuid
            });
        })
});

router.get('/designs/(:uuid)/(:zoom)/(:x)/(:y)/(:size).png', function(req, res, next) {
    authorization = extractToken(req);

    let config = {
        timeout: 10000,
        responseType:'stream',
        httpsAgent: agent
    }

    if (authorization) {
        config['headers'] = { 'authorization': authorization }
    }

    const path = req.params.uuid + '/' + req.params.zoom + '/' + req.params.x + '/' + req.params.y + '/' + req.params.size + '.png'

    req.pause();

    axios.get(appConfig.api_url + '/designs/' + path, config)
        .then(function (response) {
            req.resume();
            if (response.status == 200) {
                res.set('Cache-Control', 'public, max-age=3600, immutable');
                response.data.pipe(res, {end:true});
            } else {
                console.log("Can't load image " + error)
                res.status(500).end();
            }
        })
        .catch(function (error) {
            req.resume();
            console.log("Can't load image " + error)
            res.status(500).end();
        })
});

module.exports = router;
