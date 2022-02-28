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
    getTimestamp,
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
    state = {}

    handleUpdate = (e) => {
        e.preventDefault()

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let timestamp = Date.now()

        let script = this.state.script ? this.state.script : this.props.design.script
        let metadata = this.state.metadata ? this.state.metadata : this.props.design.metadata

        const design = { manifest: this.props.design.manifest, script: script, metadata: metadata, levels: 3 }

        component.props.handleHideUpdateDialog()
        component.props.handleHideErrorMessage()

        axios.put(component.props.config.api_url + '/v1/designs/' + this.props.uuid, design, config)
            .then(function (content) {
                if (content.status == 202 || content.status == 200) {
                    //component.props.handleDesignLoadedSuccess(design, timestamp)
                    component.props.handleShowErrorMessage("Your request has been processed")
                } else {
                    console.log("Can't create a new design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't update the design")
                }
            })
            .catch(function (error) {
                console.log("Can't update the design: " + error)
                component.props.handleShowErrorMessage("Can't update the design")
            })
    }

    handleRender = (e) => {
        e.preventDefault()

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let timestamp = Date.now()

        let script = this.state.script ? this.state.script : this.props.design.script
        let metadata = this.state.metadata ? this.state.metadata : this.props.design.metadata

        const design = { manifest: this.props.design.manifest, script: script, metadata: metadata, levels: 8 }

        component.props.handleHideUpdateDialog()
        component.props.handleHideErrorMessage()

        axios.put(component.props.config.api_url + '/v1/designs/' + this.props.uuid, design, config)
            .then(function (content) {
                if (content.status == 202 || content.status == 200) {
                    //component.props.handleDesignLoadedSuccess(design, timestamp)
                    component.props.handleShowErrorMessage("Your request has been processed")
                } else {
                    console.log("Can't create a new design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't update the design")
                }
            })
            .catch(function (error) {
                console.log("Can't update the design: " + error)
                component.props.handleShowErrorMessage("Can't update the design")
            })
    }

    handleScriptChanged = (value) => {
        this.setState({script: value})
    }

    handleMetadataChanged = (value) => {
        this.setState({metadata: value})
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
        const url = this.props.config.api_url + '/v1/designs/' + this.props.uuid + '/{z}/{x}/{y}/256.png?t=' + this.props.design.checksum

        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs/' + this.props.uuid + '.html'} titleLink={"/admin/designs.html"} titleText={"Fractals"} titleText2={this.props.uuid} browseLink={"/browse/designs/" + this.props.uuid + ".html"} browseText={"The Beauty of Chaos"}/>
                    </Grid>
                    <Grid container xs={12} justify="space-between" alignItems="center" className="container">
                        <Grid item xs={6}>
                            <Map center={[0, 0]} zoom={2} className="preview" attributionControl={false} dragging={false} zoomControl={false} scrollWheelZoom={false} touchZoom={false}>
                                {this.renderMapLayer(url)}
                            </Map>
                        </Grid>
                        <Grid item xs={6}>
                            <DesignForm script={this.props.design.script} metadata={this.props.design.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
                            <div className="controls">
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
  }
})

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    design: getDesign(state),
    timestamp: getTimestamp(state),
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
