import React from 'react'
import PropTypes from 'prop-types'

import Header from './Header'
import Footer from './Footer'

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

import DesignForm from './DesignForm'

import { connect } from 'react-redux'

import {
    getConfig,
    getAccount,
    showUpdateDesign,
    hideUpdateDesign,
    getDesign,
    getTimestamp,
    getShowUpdateDesign,
    loadDesignSuccess
} from './actions/preview'

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
            timeout: 10000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let timestamp = Date.now()

        let script = this.state.script ? this.state.script : this.props.design.script
        let metadata = this.state.metadata ? this.state.metadata : this.props.design.metadata

        const design = { manifest: this.props.design.manifest, script: script, metadata: metadata }

        component.props.handleHideUpdateDialog()

        axios.put(component.props.config.designs_command_url + '/' + this.props.uuid, design, config)
            .then(function (content) {
                if (content.status != 202) {
                    console.log("Can't update design")
                } else {
                    //component.props.handleDesignLoaded(design, timestamp)
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
        const url = this.props.config.designs_query_url + '/' + this.props.uuid + '/{z}/{x}/{y}/256.png?t=' + this.props.timestamp

        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs/' + this.props.uuid} title={'Design | ' + this.props.uuid}/>
                    </Grid>
                    <Grid item xs={12}>
                        <div className="preview-container">
                            <Map center={[0, 0]} zoom={2} className="preview">
                                {this.renderMapLayer(url)}
                            </Map>
                        </div>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer/>
                    </Grid>
                </Grid>
                {this.props.account.role == 'admin' && (
                    <div className={this.props.classes.fabcontainer}>
                        <Button variant="fab" className={this.props.classes.fab} color="primary" onClick={this.props.handleShowUpdateDialog}>
                            <EditIcon />
                        </Button>
                    </div>
                )}
                {this.props.account.role == 'admin' && (
                    <Dialog className={this.props.classes.dialog} open={this.props.show_update_design} onClose={this.props.handleHideUpdateDialog} scroll={"paper"} TransitionComponent={SlideTransition}>
                        <DialogTitle>Update Design</DialogTitle>
                        <DialogContent>
                            <DesignForm script={this.props.design.script} metadata={this.props.design.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
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
  timestamp: PropTypes.number.isRequired,
  show_update_design: PropTypes.bool.isRequired,
  classes: PropTypes.object.isRequired,
  theme: PropTypes.object.isRequired,
  uuid: PropTypes.string.isRequired
}

const styles = theme => ({
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
    show_update_design: getShowUpdateDesign(state)
})

const mapDispatchToProps = dispatch => ({
    handleShowUpdateDialog: () => {
        dispatch(showUpdateDesign())
    },
    handleHideUpdateDialog: () => {
        dispatch(hideUpdateDesign())
    },
    handleDesignLoaded: (design, timestamp) => {
        dispatch(loadDesignSuccess(design, timestamp))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(PreviewPage))
