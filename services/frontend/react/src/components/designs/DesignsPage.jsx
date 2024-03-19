import React from 'react'
import { useRef, useState, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import UpdateDesign from '../../commands/updateDesign'
import CreateDesign from '../../commands/createDesign'
import DeleteDesigns from '../../commands/deleteDesigns'

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

    const onChangeSelection = useCallback((selection) => dispatch(setDesignsSelection(selection)), [dispatch])
    const onDesignSelected = useCallback((design) => dispatch(setSelectedDesign(design)), [dispatch])
    const onShowErrorMessage = useCallback((error) => dispatch(showErrorMessage(error)), [dispatch])
    const onHideErrorMessage = useCallback(() => dispatch(hideErrorMessage()), [dispatch])
    const onHideCreateDialog = useCallback(() => dispatch(hideCreateDesign()), [dispatch])
    const onHideUpdateDialog = useCallback(() => dispatch(hideUpdateDesign()), [dispatch])
    const onHideConfirmDelete = useCallback(() => dispatch(hideDeleteDesigns()), [dispatch])

    const onCreate = useCallback(() => {
        const command = new CreateDesign(config, abortControllerRef)

        command.onCreateDesign = () => {
            onHideErrorMessage()
        }

        command.onCreateDesignSuccess = (message) => {
            onShowErrorMessage(message)
            onHideCreateDialog()
        }

        command.onCreateDesignFailure = (error) => {
            onShowErrorMessage(error)
        }

        command.run(design)
    }, [config, design, onHideErrorMessage, onShowErrorMessage, onHideCreateDialog])

    const onUpdate = useCallback((e) => {
        if (selection.length == 0) {
            console.log("No design selected")
            return;
        }

        const command = new UpdateDesign(config, abortControllerRef)

        command.onUpdateDesign = () => {
            onHideErrorMessage()
        }

        command.onUpdateDesignSuccess = (message) => {
            onShowErrorMessage(message)
            onHideUpdateDialog()
        }

        command.onUpdateDesignFailure = (error) => {
            onShowErrorMessage(error)
        }

        command.run(selection[0], design)
    }, [config, design, selection, onHideErrorMessage, onShowErrorMessage, onHideUpdateDialog])

    const onDelete = useCallback(() => {
        if (selection.length == 0) {
            return;
        }

        const command = new DeleteDesigns(config, abortControllerRef)

        command.onDeleteDesigns = () => {
            onHideErrorMessage()
        }

        command.onDeleteDesignsSuccess = (message) => {
            onShowErrorMessage(message)
            onChangeSelection([])
            onHideConfirmDelete()
        }

        command.onDeleteDesignsFailure = (error) => {
            onShowErrorMessage(error)
            onChangeSelection([])
        }

        command.run(selection)
    }, [config, selection, onHideErrorMessage, onShowErrorMessage, onHideConfirmDelete, onChangeSelection])

    const onModify = useCallback(() => {
        if (selection.length == 1) {
            window.location = config.web_url + "/admin/designs/" + selection[0] + ".html"
        }
    }, [selection])

    const onEditorChanged = useCallback((design) => {
        onDesignSelected(design);
    }, [onDesignSelected])

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
