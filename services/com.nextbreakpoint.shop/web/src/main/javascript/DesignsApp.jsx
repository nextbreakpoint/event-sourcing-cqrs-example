const React = require('react')
const ReactDOM = require('react-dom')

const Header = require('./Header')
const Footer = require('./Footer')
const Account = require('./Account')
const Designs = require('./Designs')
const NewDesign = require('./NewDesign')

const { Row, Col } = require('react-materialize')

const axios = require('axios')

const Cookies = require('universal-cookie')

const cookies = new Cookies()

const base_url = 'https://' + window.location.host

var modified = 0;

class App extends React.Component {
    constructor(props) {
        super(props)

        this.state = {role: 'anonymous', designs: [], modified: 0}

        this.loadAccount = this.loadAccount.bind(this)
        this.loadDesigns = this.loadDesigns.bind(this)
        this.reloadDesigns = this.reloadDesigns.bind(this)
        this.handleCreate = this.handleCreate.bind(this)
        this.handleDelete = this.handleDelete.bind(this)
        this.handleSelect = this.handleSelect.bind(this)
        this.handleLogin = this.handleLogin.bind(this)
        this.handleLogout = this.handleLogout.bind(this)
        this.installWatcher = this.installWatcher.bind(this)
        this.componentDidMount = this.componentDidMount.bind(this)
    }

    handleCreate(data) {
        let component = this

        let config = {
            timeout: 10000,
            headers: {'content-type': 'application/json'},
            withCredentials: true
        }

        axios.post(component.state.config.designs_url + '/api/designs', data, config)
            .then(function (response) {
                if (response.status == 201) {
                    var designs = component.state.designs.slice()

                    designs.push({uuid:response.data.uuid, selected: false})

                    component.setState(Object.assign(component.state, {designs: designs}))
                }
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleDelete() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        let promises = this.state.designs
            .filter((design) => {
                return design.selected
            }).map((design) => {
                return axios.delete(component.state.config.designs_url + '/api/designs/' + design.uuid, config)
            })

        axios.all(promises)
            .then(function (responses) {
                let deletedUuids = responses
                    .filter((res) => {
                        return res.status == 200
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                let designs = component.state.designs
                    .filter((design) => {
                        return !deletedUuids.includes(design.uuid)
                    })
                    .map((design) => {
                        return { uuid: design.uuid, selected: design.selected }
                    })

                component.setState(Object.assign(component.state, {designs: designs}))
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleSelect(uuid, selected) {
        let designs = this.state.designs
            .map((design) => { return { uuid: design.uuid, selected: (design.uuid == uuid ? selected : design.selected) }})

        this.setState(Object.assign(this.state, {designs: designs}))
    }

    handleLogin() {
        window.location = this.state.config.auth_url + "/auth/signin/admin/designs"
    }

    handleLogout() {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})

        this.setState(Object.assign(this.state, {role: 'anonymous', name: 'Stranger'}))

        this.loadDesigns();

        //window.location = this.state.config.auth_url + "/auth/signout/admin/designs"
    }

    installWatcher(offset) {
        let component = this

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource("/watch/" + offset + "/designs", { withCredentials: true })

                source.addEventListener("message",  function(event) {
                   let json = JSON.parse(event.data)

                   let offset = json.offset

                   console.log(event)

                   if (offset > component.state.modified) {
                      console.log("Reload designs")

                      component.reloadDesigns()
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
            timeout: 5000,
            withCredentials: true
        }

        axios.get(component.state.config.accounts_url + '/api/accounts/me', config)
            .then(function (response) {
                let role = response.data.role
                let name = response.data.name

                component.setState(Object.assign(component.state, {role: role, name: name}))

                component.loadDesigns()
            })
            .catch(function (error) {
                cookies.remove('token', {domain: window.location.hostname})

                component.setState(Object.assign(component.state, {role: 'anonymous', name: 'Stranger'}))

                component.loadDesigns()
            })
    }

    loadDesigns() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.state.config.designs_url + '/api/designs', config)
            .then(function (response) {
                let designs = response.data.map((uuid) => { return { uuid: uuid, selected: false }})

                let modified = response.headers['x-modified']

                component.setState(Object.assign(component.state, {designs: designs, modified: modified}))

                component.installWatcher(modified)
            })
            .catch(function (error) {
                console.log(error)

                component.setState(Object.assign(component.state, {designs: []}))
            })
    }

    reloadDesigns() {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.state.config.designs_url + '/api/designs', config)
            .then(function (response) {
                let designs = response.data.map((uuid) => { return { uuid: uuid, selected: false }})

                let modified = response.headers['x-modified']

                component.setState(Object.assign(component.state, {designs: designs, modified: modified}))
            })
            .catch(function (error) {
                console.log(error)

                component.setState(Object.assign(component.state, {designs: []}))
            })
    }

    render() {
        if (this.state.config) {
            if (this.state.role == 'admin') {
                return <div className="container s12">
                    <Row>
                        <Col s={12}>
                            <Header role={this.state.role} name={this.state.name} onLogin={this.handleLogin} onLogout={this.handleLogout}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={12}>
                            <Designs config={this.state.config} role={this.state.role} designs={this.state.designs} modified={this.state.modified} onDelete={this.handleDelete} onSelect={this.handleSelect}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={12}>
                            <NewDesign onCreate={this.handleCreate}/>
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
                            <Header role={this.state.role} name={this.state.name} onLogin={this.handleLogin} onLogout={this.handleLogout}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col s={12}>
                            <Designs config={this.state.config} role={this.state.role} designs={this.state.designs} modified={this.state.modified} onDelete={this.handleDelete} onSelect={this.handleSelect}/>
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

ReactDOM.render(<App />, document.getElementById('app-designs'))
