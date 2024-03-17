import React from 'react'
import { useRef, useState, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import LoadDesigns from '../../commands/loadDesigns'

import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Slide from '@mui/material/Slide'
import Fade from '@mui/material/Fade'
import DesignPreview from '../shared/DesignPreview'
import DesignsTable from './DesignsTable'

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

export default function DesignsPage() {
    const abortControllerRef = useRef(new AbortController())
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const designs = useSelector(getDesigns)
    const revision = useSelector(getRevision)
    const selection = useSelector(getSelection)
    const design = useSelector(getSelectedDesign)
    const isShowCreateDesign = useSelector(getShowCreateDesign)
    const isShowUpdateDesign = useSelector(getShowUpdateDesign)
    const isShowDeleteDesigns = useSelector(getShowDeleteDesigns)
    const dispatch = useDispatch()

    const onChangeSelection = (selection) => dispatch(setDesignsSelection(selection))
    const onDesignSelected = (design) => dispatch(setSelectedDesign(design))
    const onShowErrorMessage = (error) => dispatch(showErrorMessage(error))
    const onHideErrorMessage = () => dispatch(hideErrorMessage())
    const onHideCreateDialog = () => dispatch(hideCreateDesign())
    const onHideUpdateDialog = () => dispatch(hideUpdateDesign())
    const onHideConfirmDelete = () => dispatch(hideDeleteDesigns())

    const onCreate = () => {
        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.post(config.api_url + '/v1/designs', design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 201) {
                                    onShowErrorMessage("Your request has been received. The designs will be updated shortly")
                                    onHideCreateDialog()
                                } else {
                                    console.log("Can't create the design: status = " + response.status)
                                    onShowErrorMessage("Can't create the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't create the design: " + error)
                                onShowErrorMessage("Can't create the design")
                            })
                     } else {
                        console.log("Can't create the design: status = " + result.status)
                        onShowErrorMessage("Can't create the design")
                     }
                } else {
                    console.log("Can't create the design: status = " + response.status)
                    onShowErrorMessage("Can't create the design")
                }
            })
            .catch(function (error) {
                console.log("Can't create the design: " + error)
                onShowErrorMessage("Can't create the design")
            })
    }

    const onUpdate = (e) => {
        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        if (selection.length != 1) {
            console.log("Can't update the design: " + error)
            onShowErrorMessage("Can't update the design")
            return;
        }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.put(config.api_url + '/v1/designs/' + selection[0], design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    onShowErrorMessage("Your request has been received. The design will be updated shortly")
                                    onHideUpdateDialog()
                                } else {
                                    console.log("Can't update the design: status = " + response.status)
                                    onShowErrorMessage("Can't update the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't update the design: " + error)
                                onShowErrorMessage("Can't update the design")
                            })
                     } else {
                        onShowErrorMessage("Can't update the design")
                     }
                } else {
                    console.log("Can't update the design: status = " + response.status)
                    onShowErrorMessage("Can't update the design")
                }
            })
            .catch(function (error) {
                console.log("Can't update the design: " + error)
                onShowErrorMessage("Can't update the design")
            })
    }

    const onDelete = () => {
        const axiosConfig = {
            timeout: 30000,
            withCredentials: true
        }

        const promises = selection
           .map((uuid) => {
                return axios.delete(config.api_url + '/v1/designs/' + uuid + '?draft=true', axiosConfig)
            })

        onHideConfirmDelete()
        onHideErrorMessage()

        axios.all(promises)
            .then(function (responses) {
                const deletedUuids = responses
                    .filter((res) => {
                        return (res.status == 202 || res.status == 200)
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                const failedUuids = responses
                    .filter((res) => {
                        return (res.status != 202 && res.status != 200)
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                onChangeSelection([])

                if (failedUuids.length == 0) {
                    onShowErrorMessage("Your request has been received. The designs will be updated shortly")
                } else {
                    onShowErrorMessage("Can't delete the designs")
                }
            })
            .catch(function (error) {
                console.log("Can't delete the designs: " + error)
                onShowErrorMessage("Can't delete the designs")
            })
    }

    const onModify = () => {
        if (selection.length == 1) {
            window.location = config.web_url + "/admin/designs/" + selection[0] + ".html"
        }
    }

    const onEditorChanged = (design) => {
        console.log(JSON.stringify(design));

        onDesignSelected(design);
    }

    return (
        <React.Fragment>
            <DesignsTable/>
            {account.role == 'admin' && (
                <Dialog className="dialog" open={isShowCreateDesign} onClose={onHideCreateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                    <DialogTitle>Create New Design</DialogTitle>
                    <DialogContent>
                        <DesignPreview initialDesign={design} config={config} onPreviewChanged={onEditorChanged}/>
                    </DialogContent>
                    <DialogActions>
                        <Button variant="outlined" color="primary" onClick={onHideCreateDialog} color="primary">
                          Cancel
                        </Button>
                        <Button variant="outlined" color="primary" onClick={onCreate} color="primary" autoFocus>
                          Create
                        </Button>
                    </DialogActions>
                </Dialog>
            )}
            {account.role == 'admin' && (
                <Dialog className="dialog" open={isShowUpdateDesign} onClose={onHideUpdateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                    <DialogTitle>Modify Existing Design</DialogTitle>
                    <DialogContent>
                        <DesignPreview initialDesign={design} config={config} onPreviewChanged={onEditorChanged}/>
                    </DialogContent>
                    <DialogActions>
                        <Button variant="outlined" color="primary" onClick={onHideUpdateDialog} color="primary">
                          Cancel
                        </Button>
                        <Button variant="outlined" color="primary" onClick={onUpdate} color="primary" autoFocus>
                          Update
                        </Button>
                    </DialogActions>
                </Dialog>
            )}
            {account.role == 'admin' && (
                <Dialog className="dialog" open={isShowDeleteDesigns} onClose={onHideConfirmDelete} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={FadeTransition}>
                    <DialogTitle>Delete Designs</DialogTitle>
                    <DialogContent>
                        {selection.length == 1 ? (<p>Do you want to delete {selection.length} item?</p>) : (<p>Do you want to delete {selection.length} items?</p>)}
                    </DialogContent>
                    <DialogActions>
                        <Button variant="outlined" color="primary" onClick={onHideConfirmDelete} color="primary">
                          Cancel
                        </Button>
                        <Button variant="outlined" color="primary" onClick={onDelete} color="primary" autoFocus>
                          Delete
                        </Button>
                    </DialogActions>
                </Dialog>
            )}
        </React.Fragment>
    )
}
