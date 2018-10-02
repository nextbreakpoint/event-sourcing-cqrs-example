const React = require('react')
const ReactDOM = require('react-dom')

const Header = require('./Header')
const Footer = require('./Footer')
const Account = require('./Account')

const { Map, TileLayer } = require('react-leaflet')

const { Row, Col, Icon, Button } = require('react-materialize')

const axios = require('axios')

const Cookies = require('universal-cookie')

const cookies = new Cookies()

const position = [0, 0]

var uuid = "00000000-0000-0000-0000-000000000000"

const regexp = /https?:\/\/.*\/admin\/designs\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/g
const match = regexp.exec(window.location.href)

if (match != null && match.length == 2) {
    uuid = match[1]
}

const base_url = 'https://' + window.location.host

class App extends React.Component {
    constructor(props) {
        super(props)

        this.state = {role: 'anonymous', design: '', oldDesign: '', timestamp: 0}

        this.loadAccount = this.loadAccount.bind(this)
        this.setupDesign = this.setupDesign.bind(this)
        this.loadDesign = this.loadDesign.bind(this)
        this.handleLogin = this.handleLogin.bind(this)
        this.handleLogout = this.handleLogout.bind(this)
        this.handleUpdateDesign = this.handleUpdateDesign.bind(this)
        this.handleScriptChanged = this.handleScriptChanged.bind(this)
        this.handleMetadataChanged = this.handleMetadataChanged.bind(this)
        this.installWatcher = this.installWatcher.bind(this)
        this.componentDidMount = this.componentDidMount.bind(this)
    }

    handleLogin() {
        window.location = this.state.config.auth_url + "/signin/admin/designs/" + uuid
    }

    handleLogout() {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})

        this.setState(Object.assign(this.state, {role: 'anonymous', name: 'Stranger'}))

        //window.location = this.state.config.auth_url + "/signout/admin/designs/" + uuid
    }

    handleUpdateDesign(e) {
        e.preventDefault()

        let component = this

        let config = {
            timeout: 10000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        axios.put(component.state.config.designs_command_url + '/' + uuid, this.state.design, config)
            .then(function (content) {
                if (content.status != 200) {
                    console.log("Can't update design")
                }
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleScriptChanged(e) {
        e.preventDefault()
        let source = e.target
        this.setState(Object.assign(this.state, {design: {script: source.value, metadata: this.state.design.metadata, manifest: this.state.design.manifest}}))
    }

    handleMetadataChanged(e) {
        e.preventDefault()
        let source = e.target
        this.setState(Object.assign(this.state, {design: {script: this.state.design.script, metadata: source.value, manifest: this.state.design.manifest}}))
    }

    installWatcher(timestamp, uuid) {
        let component = this

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.state.config.web_url + "/watch/designs/" + timestamp + "/" + uuid, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                  component.loadDesign(timestamp)
                }

                source.addEventListener("update",  function(event) {
                   let timestamp = Number(event.lastEventId)

                   console.log(event)

                   if (component.state.timestamp == undefined || timestamp > component.state.timestamp) {
                      console.log("Reload design")

                      component.loadDesign(timestamp)
                   }
                }, false)
            } else {
                console.log("EventSource not available")
            }
        } catch (e) {
           console.log(e)
        }
    }

    componentDidMount() {
        let component = this

        let config = {
            timeout: 5000,
            withCredentials: true
        }

        axios.get(base_url + '/config', config)
            .then(function (content) {
                component.setState(Object.assign(component.state, {config: content.data}))

                component.loadAccount()
            })
            .catch(function (error) {
                console.log("Can't load config " + error)
            })
    }

    loadAccount() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.state.config.accounts_url + '/me', config)
            .then(function (content) {
                let role = content.data.role
                let name = content.data.name

                component.setState(Object.assign(component.state, {role: role, name: name}))

                component.setupDesign()
            })
            .catch(function (error) {
                cookies.remove('token', {domain: window.location.hostname})

                component.setState(Object.assign(component.state, {role: 'anonymous', name: 'Stranger'}))

                component.setupDesign()
            })
    }

    setupDesign() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        let timestamp = Date.now();

        component.installWatcher(timestamp, uuid)
    }

    loadDesign(timestamp) {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.state.config.designs_query_url + '/' + uuid, config)
            .then(function (content) {
                let envelop = content.data

                console.log(envelop)

                let design = JSON.parse(envelop.json)

                let previousDesign = component.state.oldDesign;

                let currentDesign = component.state.design;

                if (previousDesign === '' || previousDesign.manifest !== currentDesign.manifest || previousDesign.metadata !== currentDesign.metadata || previousDesign.script !== currentDesign.script) {
                    component.setState(Object.assign(component.state, {oldDesign: design, design: design, timestamp: timestamp}))
                } else {
                    console.log("No changes detected");
                }
            })
            .catch(function (error) {
                console.log(error)

                component.setState(Object.assign(component.state, {design: ''}))
            })
    }

    render() {
        if (this.state.config && this.state.timestamp > 0) {
            const url = this.state.config.designs_query_url + '/' + uuid + '/{z}/{x}/{y}/256.png?t=' + this.state.timestamp

            const parent = { label: 'Designs', link: base_url + '/admin/designs' }

            if (this.state.role == 'admin') {
                return <div className="container s12">
                    <Row>
                        <Col s={12}>
                            <Header role={this.state.role} name={this.state.name} onLogin={this.handleLogin} onLogout={this.handleLogout} parent={parent}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={8} className="center-align">
                            <div className="design-preview-container">
                                <Map center={position} zoom={2} className="design-preview z-depth-3">
                                    <TileLayer url={url} attribution='&copy; Andrea Medeghini' minZoom={2} maxZoom={6} tileSize={256} updateWhenIdle={true} updateWhenZooming={false} updateInterval={500} keepBuffer={1}/>
                                </Map>
                            </div>
                        </Col>
                        <Col s={4} className="left-align">
                            <form className="design-script">
                                <div className="input-field">
                                    <label htmlFor="script"><Icon left>mode_edit</Icon>Script</label>
                                    <textarea className="materialize-textarea" rows="20" cols="80" id="script" name="script" value={this.state.design.script} onChange={(e) => this.handleScriptChanged(e)}></textarea>
                                </div>
                                <div className="input-field">
                                    <label htmlFor="metadata"><Icon left>mode_edit</Icon>Metadata</label>
                                    <textarea className="materialize-textarea" rows="20" cols="80" id="metadata" name="metadata" value={this.state.design.metadata} onChange={(e) => this.handleMetadataChanged(e)}></textarea>
                                </div>
                                <Button waves='light' onClick={(e) => this.handleUpdateDesign(e)}>Update</Button>
                            </form>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={12}>
                            <Footer role={this.state.role} name={this.state.name}/>
                        </Col>
                    </Row>
                </div>
            } else {
                return <div className="container s12">
                    <Row>
                        <Col s={12}>
                            <Header role={this.state.role} name={this.state.name} onLogin={this.handleLogin} onLogout={this.handleLogout} parent={parent}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={8} className="center-align">
                            <div className="design-preview-container">
                                <Map center={position} zoom={2} className="design-preview z-depth-3">
                                    <TileLayer url={url} attribution='&copy; Andrea Medeghini' minZoom={2} maxZoom={6} tileSize={256} updateWhenIdle={true} updateWhenZooming={false} updateInterval={500} keepBuffer={1}/>
                                </Map>
                            </div>
                        </Col>
                        <Col s={4} className="left-align">
                            <form className="design-script">
                                <div className="input-field">
                                    <label htmlFor="script"><Icon left>mode_edit</Icon>Script</label>
                                    <textarea readonly className="materialize-textarea" rows="20" cols="80" id="script" name="script" value={this.state.design.script} onChange={(e) => this.handleScriptChanged(e)}></textarea>
                                </div>
                                <div className="input-field">
                                    <label htmlFor="metadata"><Icon left>mode_edit</Icon>Metadata</label>
                                    <textarea readonly className="materialize-textarea" rows="20" cols="80" id="metadata" name="metadata" value={this.state.design.metadata} onChange={(e) => this.handleMetadataChanged(e)}></textarea>
                                </div>
                            </form>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={12}>
                            <Footer role={this.state.role} name={this.state.name}/>
                        </Col>
                    </Row>
                </div>
            }
        } else {
            return <div className="container s12"></div>
        }
    }
}

ReactDOM.render(<App />, document.getElementById('app-preview'))
