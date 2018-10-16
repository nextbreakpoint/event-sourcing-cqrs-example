import React from 'react'
import PropTypes from 'prop-types'

import Header from './Header'
import Footer from './Footer'
import Account from './Account'

import { Map, TileLayer } from 'react-leaflet'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Button from '@material-ui/core/Button'
import Grid from '@material-ui/core/Grid'

import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'

import { connect } from 'react-redux'

import axios from 'axios'

import Cookies from 'universal-cookie'

const cookies = new Cookies()

const position = [0, 0]

var uuid = "00000000-0000-0000-0000-000000000000"

const regexp = /https?:\/\/.*\/admin\/designs\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/g
const match = regexp.exec(window.location.href)

if (match != null && match.length == 2) {
    uuid = match[1]
}

const base_url = 'https://localhost:8080'

const styles = theme => ({
  button: {
    position: 'absolute',
    bottom: theme.spacing.unit * 2,
    right: theme.spacing.unit * 2
  }
})

class PreviewPage extends React.Component {
    state = {
        design: '',
        oldDesign: '',
        timestamp: 0
    }

    componentDidMount = () => {
        let component = this

        let timestamp = Date.now();

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.props.config.web_url + "/watch/designs/" + timestamp + "/" + uuid, { withCredentials: true })

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

    loadDesign = (timestamp) => {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.props.config.designs_query_url + '/' + uuid, config)
            .then(function (content) {
                let envelop = content.data

                console.log(envelop)

                let currentDesign = JSON.parse(envelop.json)

                let previousDesign = component.state.oldDesign;

                //if (previousDesign == '' || previousDesign.manifest != currentDesign.manifest || previousDesign.metadata != currentDesign.metadata || previousDesign.script != currentDesign.script) {
                    component.setState(Object.assign(component.state, {oldDesign: currentDesign, design: currentDesign, timestamp: timestamp}))
                //} else {
                //    console.log("No changes detected");
                //}
            })
            .catch(function (error) {
                console.log(error)

                component.setState(Object.assign(component.state, {design: ''}))
            })
    }

    handleUpdateDesign = (e) => {
        e.preventDefault()

        let component = this

        let config = {
            timeout: 10000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        axios.put(component.props.config.designs_command_url + '/' + uuid, this.state.design, config)
            .then(function (content) {
                if (content.status != 200) {
                    console.log("Can't update design")
                }
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleScriptChanged = (value) => {
        this.setState(Object.assign(this.state, {design: {script: value, metadata: this.state.design.metadata, manifest: this.state.design.manifest}}))
    }

    handleMetadataChanged = (value) => {
        this.setState(Object.assign(this.state, {design: {script: this.state.design.script, metadata: value, manifest: this.state.design.manifest}}))
    }

    renderMapLayer = (url) => {
        return <TileLayer url={url} attribution='&copy; Andrea Medeghini' minZoom={2} maxZoom={6} tileSize={256} updateWhenIdle={true} updateWhenZooming={false} updateInterval={500} keepBuffer={1}/>
    }

    render() {
        const url = this.props.config.designs_query_url + '/' + uuid + '/{z}/{x}/{y}/256.png?t=' + this.state.timestamp

        const parent = { label: 'Designs', link: base_url + '/admin/designs' }

        //console.log(">> " + JSON.stringify(this.state.design))

        const design = this.state.design

        const role = this.props.account.role
        const name = this.props.account.name

        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header parent={parent}/>
                    </Grid>
                    <Grid item xs={12}>
                        <Grid item xs={8} className="center-align">
                            <div className="design-preview-container">
                                <Map center={position} zoom={2} className="design-preview z-depth-3">
                                    {this.renderMapLayer(url)}
                                </Map>
                            </div>
                        </Grid>
                        <Grid item xs={4} className="left-align">
                            <div className="input-field">
                                <p>Script</p>
                                {design.script && <ScriptEditor initialValue={design.script} readOnly={role != 'admin'} onContentChanged={this.handleScriptChanged}/>}
                            </div>
                            <div className="input-field">
                                <p>Metadata</p>
                                {design.metadata && <MetadataEditor initialValue={design.metadata} readOnly={role != 'admin'} onContentChanged={this.handleMetadataChanged}/>}
                            </div>
                            {role == 'admin' && <Button onClick={this.handleUpdateDesign}>Update</Button>}
                        </Grid>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer role={role} name={name}/>
                    </Grid>
                </Grid>
            </React.Fragment>
        )
    }
}

const mapStateToProps = state => {
    //console.log(JSON.stringify(state))

    return {
        config: state.designs.config,
        account: state.designs.account
    }
}

const mapDispatchToProps = dispatch => ({})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(PreviewPage))
