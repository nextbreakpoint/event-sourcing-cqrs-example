import React from 'react'
import PropTypes from 'prop-types'

import Header from '../shared/Header'
import Footer from '../shared/Footer'
import DesignForm from '../shared/DesignForm'

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
    showUpdateDesign,
    hideUpdateDesign,
    getDesign,
    getRevision,
    getShowUpdateDesign,
    loadDesignSuccess,
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

    handleUpload = (e) => {
        let component = this

        let formData = new FormData();
        formData.append('file', e.target.files[0]);

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'multipart/form-data'},
            withCredentials: true
        }

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/upload', formData, config)
            .then(function (response) {
                if (response.status == 200) {
                    if (response.data.errors.length == 0) {
                        let design = { manifest: response.data.manifest, metadata: response.data.metadata, script: response.data.script }
                        component.setState({design: design})
                        component.props.handleShowErrorMessage("The file has been uploaded")
                    } else {
                        component.props.handleShowErrorMessage("Can't upload the file")
                    }
                } else {
                    console.log("Can't upload the file: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't upload the file")
                }
            })
            .catch(function (error) {
                console.log("Can't upload the file: " + error)
                component.props.handleShowErrorMessage("Can't upload the file")
            })
    }

    handleDownload = (e) => {
        console.log("download")

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            responseType: "blob"
        }

        let timestamp = Date.now()

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const design = { manifest: manifest, script: script, metadata: metadata, levels: 8 }

        component.props.handleHideUpdateDialog()
        component.props.handleHideErrorMessage()

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
    }

    handleUpdate = (e) => {
        console.log("update")
//         e.preventDefault()

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let timestamp = Date.now()

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const design = { manifest: manifest, script: script, metadata: metadata, levels: 8 }

        component.props.handleHideUpdateDialog()
        component.props.handleHideErrorMessage()

        axios.put(component.props.config.api_url + '/v1/designs/' + this.props.uuid, design, config)
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
    }

    handleRender = (e) => {
        console.log("render")
//         e.preventDefault()

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let timestamp = Date.now()

        let script = this.state.design.script ? this.state.design.script : this.props.design.script
        let metadata = this.state.design.metadata ? this.state.design.metadata : this.props.design.metadata
        let manifest = this.state.design.manifest ? this.state.design.manifest : this.props.design.manifest

        const design = { manifest: manifest, script: script, metadata: metadata, levels: 8 }

        component.props.handleHideUpdateDialog()
        component.props.handleHideErrorMessage()

        axios.put(component.props.config.api_url + '/v1/designs/' + this.props.uuid, design, config)
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
    }

    handleScriptChanged = (value) => {
        console.log("changed")
        this.setState({design: {...this.state.design, script: value}})
    }

    handleMetadataChanged = (value) => {
        console.log("changed")
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
        const { classes, uuid, checksum } = this.props

        const design = (this.state.design.script && this.state.design.metadata) ? this.state.design : this.props.design

        console.log(design.script)

        const url = this.props.config.api_url + '/v1/designs/' + uuid + '/{z}/{x}/{y}/256.png?draft=true&t=' + checksum

        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs/' + uuid + '.html'} titleLink={"/admin/designs.html"} titleText={"Fractals"} titleText2={uuid} browseLink={"/browse/designs/" + uuid + ".html"} browseText={"The Beauty of Chaos"}/>
                    </Grid>
                    <Grid container xs={12} justify="space-between" alignItems="center" className="container">
                        <Grid item xs={6}>
                            <Map center={[0, 0]} zoom={2} attributionControl={false} dragging={false} zoomControl={false} scrollWheelZoom={false} touchZoom={false}>
                                {this.renderMapLayer(url)}
                            </Map>
                        </Grid>
                        <Grid item xs={6}>
                            <DesignForm script={design.script} metadata={design.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
                            <div className="controls">
                                <label htmlFor="uploadFile">
                                    <Input className={classes.uploadFile} id="uploadFile" accept="application/zip" type="file" onChange={this.handleUpload} />
                                    <Button className="button" variant="outlined" color="primary" component="span">
                                      Upload
                                    </Button>
                                </label>
                                <Button className="button" variant="outlined" color="primary" onClick={this.handleDownload}>
                                  Download
                                </Button>
                                <Button className="button" variant="outlined" color="primary" onClick={this.handleUpdate}>
                                  Update
                                </Button>
                                <Button className="button" variant="outlined" color="primary" onClick={this.handleRender}>
                                  Render
                                </Button>
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
    timestamp: PropTypes.number.isRequired,
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
  },
  uploadFile: {
    display: 'none'
  }
})

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    design: getDesign(state),
    timestamp: getRevision(state),
    show_update_design: getShowUpdateDesign(state),
    show_error_message: getShowErrorMessage(state),
    error_message: getErrorMessage(state)
})

const mapDispatchToProps = dispatch => ({
    handleShowUpdateDialog: () => {
        dispatch(showUpdateDesign())
    },
    handleHideUpdateDialog: () => {
        dispatch(hideUpdateDesign())
    },
    handleShowErrorMessage: (error) => {
        dispatch(showErrorMessage(error))
    },
    handleHideErrorMessage: () => {
        dispatch(hideErrorMessage())
    },
    handleDesignLoadedSuccess: (design, timestamp) => {
        dispatch(loadDesignSuccess(design, timestamp))
    },
    handleDesignLoadedFailure: (error) => {
        dispatch(loadDesignFailure(error))
    }
})

export default withStyles(themeStyles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(PreviewPage))

{/*                 {this.props.account.role == 'admin' && ( */}
{/*                     <div className={this.props.classes.fabcontainer}> */}
{/*                         <Button variant="fab" className={this.props.classes.fab} color="primary" onClick={this.props.handleShowUpdateDialog}> */}
{/*                             <EditIcon /> */}
{/*                         </Button> */}
{/*                     </div> */}
{/*                 )} */}
{/*                 {this.props.account.role == 'admin' && ( */}
{/*                     <Dialog className={this.props.classes.dialog} open={this.props.show_update_design} onClose={this.props.handleHideUpdateDialog} scroll={"paper"} TransitionComponent={SlideTransition}> */}
{/*                         <DialogTitle>Update Design</DialogTitle> */}
{/*                         <DialogContent> */}
{/*                             <DesignForm script={this.props.design.script} metadata={this.props.design.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/> */}
{/*                         </DialogContent> */}
{/*                         <DialogActions> */}
{/*                             <Button variant="outlined" color="primary" onClick={this.props.handleHideUpdateDialog}> */}
{/*                               Cancel */}
{/*                             </Button> */}
{/*                             <Button variant="outlined" color="primary" onClick={this.handleUpdate} autoFocus> */}
{/*                               Update */}
{/*                             </Button> */}
{/*                         </DialogActions> */}
{/*                     </Dialog> */}
{/*                 )} */}
