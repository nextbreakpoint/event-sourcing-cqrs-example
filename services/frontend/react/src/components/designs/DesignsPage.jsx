import React from 'react'
import PropTypes from 'prop-types'

import Header from '../shared/Header'
import Footer from '../shared/Footer'
import DesignPreview from '../shared/DesignPreview'
import DesignsTable from './DesignsTable'

import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Slide from '@mui/material/Slide'
import Fade from '@mui/material/Fade'
import Snackbar from '@mui/material/Snackbar'
import IconButton from '@mui/material/IconButton'

import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import CloseIcon from '@mui/icons-material/Close'

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount
} from '../../actions/account'

import {
    getDesigns,
    getRevision,
    getSelection,
    showDeleteDesigns,
    hideDeleteDesigns,
    hideCreateDesign,
    hideUpdateDesign,
    setDesignsSelection,
    getShowCreateDesign,
    getShowUpdateDesign,
    getShowDeleteDesigns,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage,
    isSelectedDesignPresent,
    getSelectedDesign,
    resetSelectedDesign,
    isUploadedDesignPresent,
    getUploadedDesign,
    resetUploadedDesign
} from '../../actions/designs'

import axios from 'axios'

function SlideTransition(props) {
  return <Slide direction="up" {...props} />
}

function FadeTransition(props) {
  return <Fade in="true" {...props} />
}

let default_script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
let default_metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
let default_manifest = "{\"pluginId\":\"Mandelbrot\"}"

let DesignsPage = class DesignsPage extends React.Component {
    state = {
        design: {
            manifest: default_manifest,
            metadata: default_metadata,
            script: default_script
        }
    }

    handleCreate = () => {
        console.log("create")

        let component = this

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let design = this.createDesign()

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.post(component.props.config.api_url + '/v1/designs', design, config)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 201) {
                                    component.props.handleShowErrorMessage("Your request has been received. The designs will be updated shortly")
                                    component.props.handleHideCreateDialog()
                                    component.props.resetUploadedDesign()
                                } else {
                                    console.log("Can't create the design: status = " + response.status)
                                    component.props.handleShowErrorMessage("Can't create the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't create the design: " + error)
                                component.props.handleShowErrorMessage("Can't create the design")
                            })
                     } else {
                        console.log("Can't create the design: status = " + result.status)
                        component.props.handleShowErrorMessage("Can't create the design")
                     }
                } else {
                    console.log("Can't create the design: status = " + response.status)
                    component.props.handleShowErrorMessage("Can't create the design")
                }
            })
            .catch(function (error) {
                console.log("Can't create the design: " + error)
                component.props.handleShowErrorMessage("Can't create the design")
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

        if (this.props.selection.length != 1) {
            console.log("Can't update the design: " + error)
            component.props.handleShowErrorMessage("Can't update the design")
            return;
        }

        let design = this.createDesign()

        let selectedDesign = this.props.designs.find((design) => design.uuid == this.props.selection[0])

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.put(component.props.config.api_url + '/v1/designs/' + selectedDesign.uuid, design, config)
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

    handleDelete = () => {
        console.log("delete")

        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        let promises = this.props.selection
            .map((uuid) => {
                return axios.delete(component.props.config.api_url + '/v1/designs/' + uuid + '?draft=true', config)
            })

        component.props.handleHideConfirmDelete()
        component.props.handleHideErrorMessage()

        axios.all(promises)
            .then(function (responses) {
                let deletedUuids = responses
                    .filter((res) => {
                        return (res.status == 202 || res.status == 200)
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                let failedUuids = responses
                    .filter((res) => {
                        return (res.status != 202 && res.status != 200)
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                let designs = component.props.designs
                    .filter((design) => {
                        return !deletedUuids.includes(design.uuid)
                    })
                    .map((design) => {
                        return { uuid: design.uuid, selected: design.selected }
                    })

                component.props.handleChangeSelected([])

                if (failedUuids.length == 0) {
                    component.props.handleShowErrorMessage("Your request has been received. The designs will be updated shortly")
                } else {
                    component.props.handleShowErrorMessage("Can't delete the designs")
                }
            })
            .catch(function (error) {
                console.log("Can't delete the designs: " + error)
                component.props.handleShowErrorMessage("Can't delete the designs")
            })
    }

    handleModify = () => {
        if (this.props.selection[0]) {
            window.location = this.props.config.web_url + "/admin/designs/" + this.props.selection[0] + ".html"
        }
    }

    handleEditorChanged = (value) => {
        if (this.props.selected_design_present == true) {
            this.props.resetSelectedDesign()
        }
        if (this.props.uploaded_design_present == true) {
            this.props.resetUploadedDesign()
        }
        this.setState({design: {...this.state.design, script: value.script, metadata: value.metadata}})
    }

    handleClose = (event, reason) => {
        if (reason === 'clickaway') {
          return
        }

        this.props.resetUploadedDesign()
        this.props.handleHideErrorMessage()
    }

    createDesign = () => {
        if (this.props.selected_design_present == true) {
            return { manifest: this.props.selected_design.manifest, script: this.props.selected_design.script, metadata: this.props.selected_design.metadata }
        }

        if (this.props.uploaded_design_present == true) {
            return { manifest: this.props.uploaded_design.manifest, script: this.props.uploaded_design.script, metadata: this.props.uploaded_design.metadata }
        }

        return { manifest: this.state.design.manifest, script: this.state.design.script, metadata: this.state.design.metadata }
    }

    render() {
        let design = this.createDesign()

        return (
            <React.Fragment>
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} browseText={"Browse fractals"} browseLink={"/browse/designs.html"}/>
                    </Grid>
                    <Grid item xs={12}>
                        <DesignsTable/>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer/>
                    </Grid>
                </Grid>
                {this.props.account.role == 'admin' && (
                    <Dialog className="dialog" open={this.props.show_create_design} onClose={this.props.handleHideCreateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                        <DialogTitle>Create New Design</DialogTitle>
                        <DialogContent>
                            <DesignPreview script={design.script} metadata={design.metadata} config={this.props.config} onEditorChanged={this.handleEditorChanged}/>
                        </DialogContent>
                        <DialogActions>
                            <Button variant="outlined" color="primary" onClick={this.props.handleHideCreateDialog} color="primary">
                              Cancel
                            </Button>
                            <Button variant="outlined" color="primary" onClick={this.handleCreate} color="primary" autoFocus>
                              Create
                            </Button>
                        </DialogActions>
                    </Dialog>
                )}
                {this.props.account.role == 'admin' && (
                    <Dialog className="dialog" open={this.props.show_update_design} onClose={this.props.handleHideUpdateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                        <DialogTitle>Modify Existing Design</DialogTitle>
                        <DialogContent>
                            <DesignPreview script={design.script} metadata={design.metadata} config={this.props.config} onEditorChanged={this.handleEditorChanged}/>
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
                {this.props.account.role == 'admin' && (
                    <Dialog className="dialog" open={this.props.show_delete_designs} onClose={this.props.handleHideConfirmDelete} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={FadeTransition}>
                        <DialogTitle>Delete Designs</DialogTitle>
                        <DialogContent>
                            {this.props.selection.length == 1 ? (<p>Do you want to delete {this.props.selection.length} item?</p>) : (<p>Do you want to delete {this.props.selection.length} items?</p>)}
                        </DialogContent>
                        <DialogActions>
                            <Button variant="outlined" color="primary" onClick={this.props.handleHideConfirmDelete} color="primary">
                              Cancel
                            </Button>
                            <Button variant="outlined" color="primary" onClick={this.handleDelete} color="primary" autoFocus>
                              Delete
                            </Button>
                        </DialogActions>
                    </Dialog>
                )}
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

DesignsPage.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    designs: PropTypes.array.isRequired,
    revision: PropTypes.number.isRequired,
    selection: PropTypes.array.isRequired,
    show_create_design: PropTypes.bool.isRequired,
    show_update_design: PropTypes.bool.isRequired,
    show_delete_designs: PropTypes.bool.isRequired,
    show_error_message: PropTypes.bool.isRequired,
    error_message: PropTypes.string.isRequired,
    selected_design_present: PropTypes.bool.isRequired,
    uploaded_design: PropTypes.object.isRequired,
    selected_design_present: PropTypes.bool.isRequired,
    uploaded_design: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    revision: getRevision(state),
    selection: getSelection(state),
    show_create_design: getShowCreateDesign(state),
    show_update_design: getShowUpdateDesign(state),
    show_delete_designs: getShowDeleteDesigns(state),
    show_error_message: getShowErrorMessage(state),
    error_message: getErrorMessage(state),
    selected_design_present: isSelectedDesignPresent(state),
    selected_design: getSelectedDesign(state),
    uploaded_design_present: isUploadedDesignPresent(state),
    uploaded_design: getUploadedDesign(state)
})

const mapDispatchToProps = dispatch => ({
    handleChangeSelected: (selection) => {
        dispatch(setDesignsSelection(selection))
    },
    handleHideConfirmDelete: () => {
        dispatch(hideDeleteDesigns())
    },
    handleHideCreateDialog: () => {
        dispatch(hideCreateDesign())
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
    resetSelectedDesign: () => {
        dispatch(resetSelectedDesign())
    },
    resetUploadedDesign: () => {
        dispatch(resetUploadedDesign())
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(DesignsPage)
