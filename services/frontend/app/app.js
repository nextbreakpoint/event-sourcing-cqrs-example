let cookieParser = require('cookie-parser');
let createError = require('http-errors');
let express = require('express');
let logger = require('morgan');
let path = require('path');
let fs = require('fs')

let indexRouter = require('./routes/index');
let adminRouter = require('./routes/admin');
let browseRouter = require('./routes/browse');
let healthRouter = require('./routes/health');

let configPath = process.env.CONFIG_PATH

let appConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'))

let app = express();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hbs');
app.set('view options', { layout: false });

app.use(logger('combined'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());

app.use(express.static(path.join(__dirname, 'public'), { immutable: true, maxAge: 3600000 }));

app.use('/', indexRouter);
app.use('/admin', adminRouter);
app.use('/browse', browseRouter);
app.use('/health', healthRouter);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
    next(createError(404));
});

// error handler
app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
        url: appConfig.client_web_url,
        message: err.message,
        error: appConfig.environment === 'development' ? err : {}
    });
});

module.exports = app;
