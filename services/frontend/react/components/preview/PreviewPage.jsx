import React from 'react'
import PropTypes from 'prop-types'

import Header from '../shared/Header'
import Footer from '../shared/Footer'
import DesignForm from '../shared/DesignForm'

import { Map, TileLayer } from 'react-leaflet'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Box from '@material-ui/core/Box'
import Button from '@material-ui/core/Button'
import Typography from '@material-ui/core/Typography'
import Grid from '@material-ui/core/Grid'
import Dialog from '@material-ui/core/Dialog'
import DialogActions from '@material-ui/core/DialogActions'
import DialogContent from '@material-ui/core/DialogContent'
import DialogTitle from '@material-ui/core/DialogTitle'
import Slide from '@material-ui/core/Slide'
import Fade from '@material-ui/core/Fade'
import Snackbar from '@material-ui/core/Snackbar'
import IconButton from '@material-ui/core/IconButton'
import Input from '@material-ui/core/Input'

import EditIcon from '@material-ui/icons/Edit'
import CloseIcon from '@material-ui/icons/Close'

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount
} from '../../actions/account'

import {
    getDesign,
    getRevision,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage
} from '../../actions/preview'

import axios from 'axios'

function SlideTransition(props) {
  return <Slide direction="up" {...props} />
}

function FadeTransition(props) {
  return <Fade in="true" {...props} />
}

let PreviewPage = class PreviewPage extends React.Component {
    state = {
        design: {}
    }

    handleDownload = (e) => {
        console.log("download")

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const design = { manifest: manifest, script: script, metadata: metadata }

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        let config = {
                            timeout: 30000,
                            metadata: {'content-type': 'application/json'},
                            withCredentials: true,
                            responseType: "blob"
                        }

                        axios.post(component.props.config.api_url + '/v1/designs/download', design, config)
                            .then(function (response) {
                                if (response.status == 200) {
                                    let url = window.URL.createObjectURL(response.data);
                                    let a = document.createElement('a');
                                    a.href = url;
                                    a.download = component.props.uuid + '.zip';
                                    a.click();
                                    component.props.handleShowErrorMessage("The design has been downloaded")
                                } else {
                                    console.log("Can't download the design: status = " + response.status)
                                    component.props.handleShowErrorMessage("Can't download the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't download the design: " + error)
                                component.props.handleShowErrorMessage("Can't download the design")
                            })
                     } else {
                        component.props.handleShowErrorMessage("Can't download the design")
                     }
                } else {
                    console.log("Can't download the design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't download the design")
                }
            })
            .catch(function (error) {
                console.log("Can't download the design: " + error)
                component.props.handleShowErrorMessage("Can't download the design")
            })
    }

    handleUpdate = (e) => {
        console.log("update")

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest
        let published = this.state.design.published ? true : false

        const design = { manifest: manifest, script: script, metadata: metadata, published: published }

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.put(component.props.config.api_url + '/v1/designs/' + component.props.uuid, design, config)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    component.props.handleShowErrorMessage("Your request has been received. The design will be updated shortly")
                                } else {
                                    console.log("Can't update the design: status = " + response.status)
                                    component.props.handleShowErrorMessage("Can't update the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't update the design: " + error)
                                component.props.handleShowErrorMessage("Can't update the design")
                            })
                     } else {
                        component.props.handleShowErrorMessage("Can't update the design")
                     }
                } else {
                    console.log("Can't update the design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't update the design")
                }
            })
            .catch(function (error) {
                console.log("Can't update the design: " + error)
                component.props.handleShowErrorMessage("Can't update the design")
            })
    }

    handlePublish = (e) => {
        console.log("publish")

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const design = { manifest: manifest, script: script, metadata: metadata, published: true }

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.put(component.props.config.api_url + '/v1/designs/' + component.props.uuid, design, config)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    component.props.handleShowErrorMessage("Your request has been received. The design will be updated shortly")
                                } else {
                                    console.log("Can't publish the design: status = " + response.status)
                                    component.props.handleShowErrorMessage("Can't publish the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't publish the design: " + error)
                                component.props.handleShowErrorMessage("Can't publish the design")
                            })
                     } else {
                        component.props.handleShowErrorMessage("Can't publish the design")
                     }
                } else {
                    console.log("Can't publish the design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't publish the design")
                }
            })
            .catch(function (error) {
                console.log("Can't publish the design: " + error)
                component.props.handleShowErrorMessage("Can't publish the design")
            })
    }

    handleUnpublish = (e) => {
        console.log("unpublish")

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const design = { manifest: manifest, script: script, metadata: metadata, published: false }

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.put(component.props.config.api_url + '/v1/designs/' + component.props.uuid, design, config)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    component.props.handleShowErrorMessage("Your request has been received. The design will be updated shortly")
                                } else {
                                    console.log("Can't unpublish the design: status = " + response.status)
                                    component.props.handleShowErrorMessage("Can't unpublish the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't unpublish the design: " + error)
                                component.props.handleShowErrorMessage("Can't unpublish the design")
                            })
                     } else {
                        component.props.handleShowErrorMessage("Can't unpublish the design")
                     }
                } else {
                    console.log("Can't unpublish the design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't unpublish the design")
                }
            })
            .catch(function (error) {
                console.log("Can't unpublish the design: " + error)
                component.props.handleShowErrorMessage("Can't unpublish the design")
            })
    }

    handleScriptChanged = (value) => {
        this.setState({design: {...this.state.design, script: value}})
    }

    handleMetadataChanged = (value) => {
        this.setState({design: {...this.state.design, metadata: value}})
    }

    handleClose = (event, reason) => {
        if (reason === 'clickaway') {
          return
        }

        this.props.handleHideErrorMessage()
    }

    renderMapLayer = (url) => {
        return <TileLayer url={url} detectRetina={true} bounds={[[-180, -180],[180, 180]]} noWrap={true} minZoom={1} maxZoom={2} tileSize={256} updateWhenIdle={true} updateWhenZooming={false} updateInterval={500} keepBuffer={2}/>
    }

    render() {
        const { classes, uuid, design } = this.props

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata

        const url = this.props.config.api_url + '/v1/designs/' + uuid + '/{z}/{x}/{y}/256.png?draft=true&t=' + design.checksum + '&r=' + design.preview_percentage

        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs/' + uuid + '.html'} titleLink={"/admin/designs.html"} titleText={"Fractals"} titleText2={uuid} browseLink={"/browse/designs/" + uuid + ".html"} browseText={"The Beauty of Chaos"}/>
                    </Grid>
                    <Grid container xs={12} justify="space-between" alignItems="center" className="container">
                        <Grid item xs={6}>
                            <div className="preview">
                                <Map center={[0, 0]} zoom={2} attributionControl={false} dragging={false} zoomControl={false} doubleClickZoom={false} scrollWheelZoom={false} touchZoom={false} detectRetina={false}>
                                    {this.renderMapLayer(url)}
                                </Map>
                            </div>
                        </Grid>
                        <Grid item xs={6}>
                            <div className="details">
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">UUID: {design.uuid}</Typography>
                                </div>
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">Checksum: {design.checksum}</Typography>
                                </div>
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">Revision: {design.revision}</Typography>
                                </div>
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">Created: {design.created}</Typography>
                                </div>
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">Updated: {design.updated}</Typography>
                                </div>
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">Draft: {design.draft ? 'Yes' : 'No'}</Typography>
                                </div>
                                <div class="details-item">
                                    <Typography variant="body" color="inherit">Completed: {design.percentage}%</Typography>
                                </div>
                            </div>
                        </Grid>
                    </Grid>
                    <Grid container xs={12} justify="space-between" alignItems="top-center" className="container">
                        <Grid item xs={12}>
                            <DesignForm script={script} metadata={metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
                            <div className="controls">
                                {this.props.account.role == 'admin' && (
                                    <Button className="button" variant="outlined" color="primary" onClick={this.handleDownload}>
                                      Download
                                    </Button>
                                )}
                                {this.props.account.role == 'admin' && (
                                    <Button className="button" variant="outlined" color="primary" onClick={this.handleUpdate}>
                                      Update
                                    </Button>
                                )}
                                {this.props.account.role == 'admin' && (
                                    <Button disabled={design.published == true} className="button" variant="outlined" color="primary" onClick={this.handlePublish}>
                                      Publish
                                    </Button>
                                )}
                                {this.props.account.role == 'admin' && (
                                    <Button disabled={design.published == false} className="button" variant="outlined" color="primary" onClick={this.handleUnpublish}>
                                      Unpublish
                                    </Button>
                                )}
                            </div>
                        </Grid>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer/>
                    </Grid>
                </Grid>
                <Snackbar
                  anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                  }}
                  open={this.props.show_error_message}
                  autoHideDuration={6000}
                  onClose={this.handleClose}
                  ContentProps={{
                    'aria-describedby': 'message-id',
                  }}
                  message={<span id="message-id">{this.props.error_message}</span>}
                  action={[
                    <IconButton
                      key="close"
                      aria-label="Close"
                      color="inherit"
                      onClick={this.handleClose}
                    >
                      <CloseIcon />
                    </IconButton>
                  ]}
                />
            </React.Fragment>
        )
    }
}

PreviewPage.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    design: PropTypes.object.isRequired,
    revision: PropTypes.number.isRequired,
    show_update_design: PropTypes.bool.isRequired,
    show_error_message: PropTypes.bool.isRequired,
    error_message: PropTypes.string.isRequired,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired,
    uuid: PropTypes.string.isRequired
}

const styles = {}

const themeStyles = theme => ({
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
  }
})

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    design: getDesign(state),
    revision: getRevision(state),
    show_error_message: getShowErrorMessage(state),
    error_message: getErrorMessage(state)
})

const mapDispatchToProps = dispatch => ({
    handleShowErrorMessage: (error) => {
        dispatch(showErrorMessage(error))
    },
    handleHideErrorMessage: () => {
        dispatch(hideErrorMessage())
    }
})

export default withStyles(themeStyles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(PreviewPage))
