import React from 'react'
import { useRef, useState, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import DownloadDesign from '../../commands/downloadDesign'
import UpdateDesign from '../../commands/updateDesign'

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
import Header from '../shared/Header'
import Footer from '../shared/Footer'
import DesignPreview from '../shared/DesignPreview'
import { MapContainer, TileLayer, useMap } from 'react-leaflet'

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

function SlideTransition(props) {
  return <Slide direction="up" {...props} />
}

function FadeTransition(props) {
  return <Fade in="true" {...props} />
}

export default function DesignPage({ uuid }) {
    const abortControllerRef = useRef(new AbortController())
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const design = useSelector(getDesign)
    const revision = useSelector(getRevision)
    const errorMessage = useSelector(getErrorMessage)
    const showUpdateDialog = useSelector(getShowUpdateDesign)
    const dispatch = useDispatch()
    const [ editedDesign, setEditedDesign ] = useState(design)

    const onShowErrorMessage = (error) => dispatch(showErrorMessage(error))
    const onHideErrorMessage = () => dispatch(hideErrorMessage())
    const onShowUpdateDialog = () => dispatch(showUpdateDesign())
    const onHideUpdateDialog = () => dispatch(hideUpdateDesign())

    const onModify = (e) => {
        onShowUpdateDialog()
    }

    const onDownload = (e) => {
        const command = new DownloadDesign(config, abortControllerRef)

        command.onDownloadDesign = () => {
            onHideErrorMessage()
        }

        command.onDownloadDesignSuccess = (message) => {
            onShowErrorMessage(message)
        }

        command.onDownloadDesignFailure = (error) => {
            onShowErrorMessage(error)
        }

        const newDesign = {
            manifest: design.manifest,
            metadata: design.metadata,
            script: design.script
        }

        command.run(newDesign)
    }

    const onUpdate = (e) => {
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

        const newDesign = {
            manifest: editedDesign.manifest,
            metadata: editedDesign.metadata,
            script: editedDesign.script
        }

        command.run(uuid, newDesign)
    }

    const onPublish = (e) => {
        const command = new UpdateDesign(config, abortControllerRef)

        command.onUpdateDesign = () => {
            onHideErrorMessage()
        }

        command.onUpdateDesignSuccess = (message) => {
            onShowErrorMessage(message)
        }

        command.onUpdateDesignFailure = (error) => {
            onShowErrorMessage(error)
        }

        const newDesign = {
            manifest: design.manifest,
            metadata: design.metadata,
            script: design.script,
            published: true
        }

        command.run(uuid, newDesign)
    }

    const onUnpublish = (e) => {
        const command = new UpdateDesign(config, abortControllerRef)

        command.onUpdateDesign = () => {
            onHideErrorMessage()
        }

        command.onUpdateDesignSuccess = (message) => {
            onShowErrorMessage(message)
        }

        command.onUpdateDesignFailure = (error) => {
            onShowErrorMessage(error)
        }

        const newDesign = {
            manifest: design.manifest,
            metadata: design.metadata,
            script: design.script,
            published: false
        }

        command.run(uuid, newDesign)
    }

    const onEditorChanged = (value) => {
        setEditedDesign({...design, script: value.script, metadata: value.metadata})
    }

    return (
        <React.Fragment>
            <Grid container xs={12} justify="space-between" alignItems="center" className="container">
                <Grid item xs={6}>
                    <div className="design-preview">
                        <MapContainer center={[0, 0]} zoom={2} attributionControl={false} dragging={false} zoomControl={false} doubleClickZoom={false} scrollWheelZoom={false} touchZoom={false}>
                            <TileLayer
                                url={config.api_url + '/v1/designs/' + uuid + '/{z}/{x}/{y}/256.png?draft=true&t=' + design.checksum + '&r=' + design.preview_percentage}
                                detectRetina={false}
                                bounds={[[-180, -180],[180, 180]]}
                                noWrap={true}
                                minZoom={2}
                                maxZoom={2}
                                tileSize={256}
                                updateWhenIdle={true}
                                updateWhenZooming={false}
                                updateInterval={500}
                                keepBuffer={2}
                            />
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
                        {account.role == 'admin' && (
                            <Button className="button" variant="outlined" color="primary" onClick={onDownload}>
                              Download
                            </Button>
                        )}
                        {account.role == 'admin' && (
                            <Button className="button" variant="outlined" color="primary" onClick={onModify}>
                              Modify
                            </Button>
                        )}
                        {account.role == 'admin' && (
                            <Button disabled={design.published == true} className="button" variant="outlined" color="primary" onClick={onPublish}>
                              Publish
                            </Button>
                        )}
                        {account.role == 'admin' && (
                            <Button disabled={design.published == false} className="button" variant="outlined" color="primary" onClick={onUnpublish}>
                              Unpublish
                            </Button>
                        )}
                    </div>
                </Grid>
            </Grid>
            {account.role == 'admin' && (
                <Dialog className="dialog" open={showUpdateDialog} onClose={onHideUpdateDialog} scroll={"paper"} maxWidth={"xl"} fullWidth={true} TransitionComponent={SlideTransition}>
                    <DialogTitle>Modify Existing Design</DialogTitle>
                    <DialogContent>
                        <DesignPreview initialDesign={design} onPreviewChanged={onEditorChanged}/>
                    </DialogContent>
                    <DialogActions>
                        <Button variant="outlined" color="primary" color="primary" onClick={onHideUpdateDialog}>
                          Cancel
                        </Button>
                        <Button variant="outlined" color="primary" color="primary" onClick={onUpdate} autoFocus>
                          Update
                        </Button>
                    </DialogActions>
                </Dialog>
            )}
        </React.Fragment>
    )
}
