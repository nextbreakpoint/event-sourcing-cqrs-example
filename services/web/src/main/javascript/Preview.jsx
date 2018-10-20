import React from 'react'
import PropTypes from 'prop-types'

import { Map, TileLayer } from 'react-leaflet'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Button from '@material-ui/core/Button'
import Grid from '@material-ui/core/Grid'
import Dialog from '@material-ui/core/Dialog'
import DialogActions from '@material-ui/core/DialogActions'
import DialogContent from '@material-ui/core/DialogContent'
import DialogTitle from '@material-ui/core/DialogTitle'
import Slide from '@material-ui/core/Slide'
import Fade from '@material-ui/core/Fade'

import EditIcon from '@material-ui/icons/Edit'

import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'
import DesignForm from './DesignForm'

import { connect } from 'react-redux'

import { setDesign, showUpdateDesign, hideUpdateDesign } from './actions/preview'

import axios from 'axios'

const position = [0, 0]

var uuid = "00000000-0000-0000-0000-000000000000"

const regexp = /https?:\/\/.*\/admin\/designs\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/g
const match = regexp.exec(window.location.href)

if (match != null && match.length == 2) {
    uuid = match[1]
}

const base_url = 'https://localhost:8080'

function SlideTransition(props) {
  return <Slide direction="up" {...props} />
}

function FadeTransition(props) {
  return <Fade in="true" {...props} />
}

class Preview extends React.Component {
    state = {}

    componentDidMount = () => {
        let component = this

        let timestamp = Date.now()

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

                   if (component.props.timestamp == undefined || timestamp > component.props.timestamp) {
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

                let design = JSON.parse(envelop.json)

                if (component.props.design == undefined || design.script != component.props.design.script || design.metadata != component.props.design.metadata || design.manifest != component.props.design.manifest) {
                    component.props.handleDesignLoaded(design, timestamp)
                }
            })
            .catch(function (error) {
                console.log(error)

                component.props.handleDesignLoaded(undefined, timestamp)
            })
    }

    handleUpdate = (e) => {
        e.preventDefault()

        let component = this

        let config = {
            timeout: 10000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        component.props.handleHideUpdateDialog()

        let timestamp = Date.now()

        let script = this.state.script ? this.state.script : this.props.design.script
        let metadata = this.state.metadata ? this.state.metadata : this.props.design.metadata

        const design = { manifest: this.props.design.manifest, script: script, metadata: metadata }

        axios.put(component.props.config.designs_command_url + '/' + uuid, design, config)
            .then(function (content) {
                if (content.status != 202) {
                    console.log("Can't update design")
                } else {
                    component.props.handleDesignLoaded(design, timestamp)
                }
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleScriptChanged = (value) => {
        this.setState({script: value})
    }

    handleMetadataChanged = (value) => {
        this.setState({metadata: value})
    }

    renderMapLayer = (url) => {
        return <TileLayer url={url} attribution='&copy; Andrea Medeghini' detectRetina={true} bounds={[[-180, -180],[180, 180]]} noWrap={true} minZoom={2} maxZoom={10} tileSize={256} updateWhenIdle={true} updateWhenZooming={false} updateInterval={500} keepBuffer={2}/>
    }

    render() {
        const url = this.props.config.designs_query_url + '/' + uuid + '/{z}/{x}/{y}/256.png?t=' + this.props.timestamp

        const parent = { label: 'Designs', link: base_url + '/admin/designs' }

        const role = this.props.account.role
        const name = this.props.account.name
        const design = this.props.design

        return (
            design ? (
                <React.Fragment>
                    <div className="preview-container">
                        <Map center={position} zoom={2} className="preview">
                            {this.renderMapLayer(url)}
                        </Map>
                    </div>
                    {role == 'admin' && (
                        <div className={this.props.classes.fabcontainer}>
                            <Button variant="fab" className={this.props.classes.fab} color="primary" onClick={this.props.handleShowUpdateDialog}>
                                <EditIcon />
                            </Button>
                        </div>
                    )}
                    {role == 'admin' && (
                        <Dialog className={this.props.classes.dialog} open={this.props.show_update_design} onClose={this.props.handleHideUpdateDialog} scroll={"paper"} TransitionComponent={SlideTransition}>
                            <DialogTitle>Update Design</DialogTitle>
                            <DialogContent>
                                <DesignForm script={design.script} metadata={design.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
                            </DialogContent>
                            <DialogActions>
                                <Button variant="outlined" color="primary" onClick={this.props.handleHideUpdateDialog} color="primary">
                                  Cancel
                                </Button>
                                <Button variant="outlined" color="primary" onClick={this.handleUpdate} color="primary" autoFocus>
                                  Update
                                </Button>
                            </DialogActions>
                        </Dialog>
                    )}
                </React.Fragment>
            ) : (
                <React.Fragment>
                    <Grid container justify="space-between" alignItems="center">
                        <Grid item xs={12}>
                            <p>Loading design...</p>
                        </Grid>
                    </Grid>
                </React.Fragment>
            )
        )
    }
}

Preview.propTypes = {
  config: PropTypes.object.isRequired,
  design: PropTypes.object,
  timestamp: PropTypes.number.isRequired,
  account: PropTypes.object.isRequired,
  show_update_design: PropTypes.bool.isRequired,
  classes: PropTypes.object.isRequired,
  theme: PropTypes.object.isRequired
}

const mapStateToProps = state => {
    //console.log(JSON.stringify(state))

    return {
        config: state.designs.config,
        account: state.designs.account,
        design: state.preview.design,
        timestamp: state.preview.timestamp,
        show_update_design: state.preview.show_update_design,
    }
}

const mapDispatchToProps = dispatch => ({
    handleShowUpdateDialog: () => {
        dispatch(showUpdateDesign())
    },
    handleHideUpdateDialog: () => {
        dispatch(hideUpdateDesign())
    },
    handleDesignLoaded: (design, timestamp) => {
        dispatch(setDesign(design, timestamp))
    }
})

const styles = theme => ({
  button: {
    position: 'absolute',
    bottom: theme.spacing.unit * 2,
    right: theme.spacing.unit * 2
  },
  fabcontainer: {
    marginLeft: '-25%',
    width: '50%',
    left: '50%',
    position: 'fixed',
    zIndex: 1000,
    bottom: theme.spacing.unit * 2,
    textAlign: 'center'
  },
  fab: {
    margin: theme.spacing.unit
  },
  dialog: {
  }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Preview))
