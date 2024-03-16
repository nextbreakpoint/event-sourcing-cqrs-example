import React from 'react'
import PropTypes from 'prop-types'

import Header from '../shared/Header'
import Footer from '../shared/Footer'
import DesignPreview from '../shared/DesignPreview'

import { MapContainer, TileLayer, useMap } from 'react-leaflet'

import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Grid from '@mui/material/Grid'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Slide from '@mui/material/Slide'
import Fade from '@mui/material/Fade'

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
    showUpdateDesign,
    hideUpdateDesign,
    getShowUpdateDesign,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage,
    hideUpdateDialog
} from '../../actions/design'

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

    handleModify = (e) => {
        console.log("modify")

        this.props.handleShowUpdateDialog()
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
                                    component.props.handleHideUpdateDialog()
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
                        console.log("Can't publish the design: " + result.status)
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
                        console.log("Can't unpublish the design: " + result.status)
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

    handleEditorChanged = (value) => {
        this.setState({design: {...this.state.design, script: value.script, metadata: value.metadata}})
    }

    render() {
        const { uuid, design } = this.props

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const url = this.props.config.api_url + '/v1/designs/' + uuid + '/{z}/{x}/{y}/256.png?draft=true&t=' + design.checksum + '&r=' + design.preview_percentage

        return (
            <React.Fragment>
                <Grid container xs={12} justify="space-between" alignItems="center" className="container">
                    <Grid item xs={6}>
                        <div className="design-preview">
                            <MapContainer center={[0, 0]} zoom={2} attributionControl={false} dragging={false} zoomControl={false} doubleClickZoom={false} scrollWheelZoom={false} touchZoom={false}>
                                <TileLayer url={url} detectRetina={false} bounds={[[-180, -180],[180, 180]]} noWrap={true} minZoom={2} maxZoom={2} tileSize={256} updateWhenIdle={true} updateWhenZooming={false} updateInterval={500} keepBuffer={2}/>
                            </MapContainer>
                        </div>
                    </Grid>
                    <Grid item xs={6}>
                        <div className="design-details">
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">UUID</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.uuid}</Typography></div>
                            </div>
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">Checksum</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.checksum}</Typography></div>
                            </div>
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">Revision</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.revision}</Typography></div>
                            </div>
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">Created</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.created}</Typography></div>
                            </div>
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">Updated</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.updated}</Typography></div>
                            </div>
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">Draft</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.draft ? 'Yes' : 'No'}</Typography></div>
                            </div>
                            <div class="details-item">
                                <div><Typography variant="body" color="inherit" class="details-label">Completed</Typography></div>
                                <div><Typography variant="body" color="inherit" class="details-value">{design.percentage}%</Typography></div>
                            </div>
                        </div>
                    </Grid>
                </Grid>
                <Grid container xs={12} justify="space-between" alignItems="top-center" className="container">
                    <Grid item xs={12} className="design-editor">
                        <div className="design-controls">
                            {this.props.account.role == 'admin' && (
                                <Button className="button" variant="outlined" color="primary" onClick={this.handleDownload}>
                                  Download
                                </Button>
                            )}
                            {this.props.account.role == 'admin' && (
                                <Button className="button" variant="outlined" color="primary" onClick={this.handleModify}>
                                  Modify
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
                {this.props.account.role == 'admin' && (
                    <Dialog className="dialog" open={this.props.show_update_design} onClose={this.props.handleHideUpdateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                        <DialogTitle>Modify Existing Design</DialogTitle>
                        <DialogContent>
                            <DesignPreview initialDesign={design} onPreviewChanged={this.handleEditorChanged}/>
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
    uuid: PropTypes.string.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    design: getDesign(state),
    revision: getRevision(state),
    show_update_design: getShowUpdateDesign(state),
    show_error_message: getShowErrorMessage(state),
    error_message: getErrorMessage(state)
})

const mapDispatchToProps = dispatch => ({
    handleShowErrorMessage: (error) => {
        dispatch(showErrorMessage(error))
    },
    handleHideErrorMessage: () => {
        dispatch(hideErrorMessage())
    },
    handleShowUpdateDialog: () => {
        dispatch(showUpdateDesign())
    },
    handleHideUpdateDialog: () => {
        dispatch(hideUpdateDesign())
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(PreviewPage)
