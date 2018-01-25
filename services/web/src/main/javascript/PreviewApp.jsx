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

        this.state = {role: 'anonymous', design: '', modified: 0}

        this.loadAccount = this.loadAccount.bind(this)
        this.loadDesign = this.loadDesign.bind(this)
        this.reloadDesign = this.reloadDesign.bind(this)
        this.handleLogin = this.handleLogin.bind(this)
        this.handleLogout = this.handleLogout.bind(this)
        this.handleUpdateDesign = this.handleUpdateDesign.bind(this)
        this.handleScriptChanged = this.handleScriptChanged.bind(this)
        this.handleMetadataChanged = this.handleMetadataChanged.bind(this)
        this.installWatcher = this.installWatcher.bind(this)
        this.componentDidMount = this.componentDidMount.bind(this)
    }

    handleLogin() {
        window.location = this.state.config.auth_url + "/auth/signin/admin/designs/" + uuid
    }

    handleLogout() {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})

        this.setState(Object.assign(this.state, {role: 'anonymous', name: 'Stranger'}))

        //window.location = this.state.config.auth_url + "/auth/signout/admin/designs/" + uuid
    }

    handleUpdateDesign(e) {
        e.preventDefault()

        let component = this

        let config = {
            timeout: 10000,
            headers: {'content-type': 'application/json'},
            withCredentials: true
        }

        axios.put(component.state.config.designs_url + '/api/designs/' + uuid, this.state.design, config)
            .then(function (response) {
                if (response.status != 200) {
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
        this.setState(Object.assign(this.state, {design: {script: source.value, metadata: this.state.design.metadata, manifest: this.state.design.manifest} }))
    }

    handleMetadataChanged(e) {
        e.preventDefault()
        let source = e.target
        this.setState(Object.assign(this.state, {design: {script: this.state.design.script, metadata: source.value, manifest: this.state.design.manifest} }))
    }

    installWatcher(offset, uuid) {
        let component = this

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource("/watch/" + offset + "/designs/" + uuid, { withCredentials: true })

                source.addEventListener("message",  function(event) {
                   let json = JSON.parse(event.data)

                   let offset = json.offset

                   console.log(event)

                   if (offset > component.state.modified) {
                      console.log("Reload design")

                      component.reloadDesign()
                   }
                }, false)

                source.onerror = function(error) {
                   console.log(error)
                }
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
            .then(function (response) {
                component.setState(Object.assign(component.state, {config: response.data}))

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

        axios.get(component.state.config.accounts_url + '/api/accounts/me', config)
            .then(function (response) {
                let role = response.data.role
                let name = response.data.name

                component.setState(Object.assign(component.state, {role: role, name: name}))

                component.loadDesign()
            })
            .catch(function (error) {
                cookies.remove('token', {domain: window.location.hostname})

                component.setState(Object.assign(component.state, {role: 'anonymous', name: 'Stranger'}))

                component.loadDesign()
            })
    }

    loadDesign() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.state.config.designs_url + '/api/designs/' + uuid, config)
            .then(function (response) {
                let envelop = response.data

                let modified = response.headers['x-modified']

                console.log(envelop)

                let design = JSON.parse(envelop.json)

                component.setState(Object.assign(component.state, {design: design, modified: modified}))

                component.installWatcher(modified, uuid)
            })
            .catch(function (error) {
                console.log(error)

                component.setState(Object.assign(component.state, {design: ''}))
            })
    }

    reloadDesign() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.state.config.designs_url + '/api/designs/' + uuid, config)
            .then(function (response) {
                let envelop = response.data

                let modified = response.headers['x-modified']

                console.log(envelop)

                let design = JSON.parse(envelop.json)

                component.setState(Object.assign(component.state, {design: design, modified: modified}))
            })
            .catch(function (error) {
                console.log(error)

                component.setState(Object.assign(component.state, {design: ''}))
            })
    }

    render() {
        if (this.state.config) {
            const url = this.state.config.designs_url + '/api/designs/' + uuid + '/{z}/{x}/{y}/256.png?t=' + this.state.modified

            const parent = { label: 'Designs', link: base_url + '/admin/designs' }

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
            return <div className="container s12"></div>
        }
    }
}

ReactDOM.render(<App />, document.getElementById('app-preview'))
