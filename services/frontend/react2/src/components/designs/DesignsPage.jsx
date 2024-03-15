import React from 'react'
import PropTypes from 'prop-types'

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
    getSelectedDesign,
    setSelectedDesign
} from '../../actions/designs'

import axios from 'axios'

function SlideTransition(props) {
  return <Slide direction="up" {...props} />
}

function FadeTransition(props) {
  return <Fade in="true" {...props} />
}

let DesignsPage = class DesignsPage extends React.Component {
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

        let uuid = this.props.selection[0]

        component.props.handleHideErrorMessage()

        axios.post(component.props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.put(component.props.config.api_url + '/v1/designs/' + uuid, design, config)
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

                component.props.handleChangeSelection([])

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
        if (this.props.selection.length == 1) {
            window.location = this.props.config.web_url + "/admin/designs/" + this.props.selection[0] + ".html"
        }
    }

    handleEditorChanged = (design) => {
        console.log("changed " + JSON.stringify(design));

        this.props.handleDesignSelected(design);
    }

    createDesign = () => {
        return this.props.selected_design
    }

    render() {
        let design = this.createDesign()

        return (
            <React.Fragment>
                <DesignsTable/>
                {this.props.account.role == 'admin' && (
                    <Dialog className="dialog" open={this.props.show_create_design} onClose={this.props.handleHideCreateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                        <DialogTitle>Create New Design</DialogTitle>
                        <DialogContent>
                            <DesignPreview initialDesign={design} config={this.props.config} onPreviewChanged={this.handleEditorChanged}/>
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
                            <DesignPreview initialDesign={design} config={this.props.config} onPreviewChanged={this.handleEditorChanged}/>
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
    selected_design: PropTypes.object.isRequired
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
    selected_design: getSelectedDesign(state)
})

const mapDispatchToProps = dispatch => ({
    handleChangeSelection: (selection) => {
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
    handleDesignSelected: (design) => {
        dispatch(setSelectedDesign(design))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(DesignsPage)
