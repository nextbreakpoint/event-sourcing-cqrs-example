import React from 'react'
import { useRef, useState, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import LoadDesign from '../../commands/loadDesign'

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

export default function DesignPage({ uuid }) {
    const abortControllerRef = useRef(new AbortController())
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const initialDesign = useSelector(getDesign)
    const errorMessage = useSelector(getErrorMessage)
    const showUpdateDialog = useSelector(getShowUpdateDesign)
    const dispatch = useDispatch()
    const [ design, setDesign ] = useState(initialDesign)

    const onShowErrorMessage = (error) => dispatch(showErrorMessage(error))
    const onHideErrorMessage = () => dispatch(hideErrorMessage())
    const onShowUpdateDialog = () => dispatch(showUpdateDesign())
    const onHideUpdateDialog = () => dispatch(hideUpdateDesign())

    const onModify = (e) => {
        onShowUpdateDialog()
    }

    const onDownload = (e) => {
        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        const newDesign = { manifest: design.manifest, metadata: design.metadata, script: design.script }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/validate', newDesign, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        const axiosConfigUpload = {
                            timeout: 30000,
                            metadata: {'content-type': 'application/json'},
                            withCredentials: true,
                            responseType: "blob"
                        }
                        axios.post(config.api_url + '/v1/designs/download', newDesign, axiosConfigUpload)
                            .then(function (response) {
                                if (response.status == 200) {
                                    const url = window.URL.createObjectURL(response.data);
                                    const a = document.createElement('a');
                                    a.href = url;
                                    a.download = uuid + '.zip';
                                    a.click();
                                    onShowErrorMessage("The design has been downloaded")
                                } else {
                                    console.log("Can't download the design: status = " + response.status)
                                    onShowErrorMessage("Can't download the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't download the design: " + error)
                                onShowErrorMessage("Can't download the design")
                            })
                     } else {
                        console.log("Can't download the design: status = " + result.status)
                        onShowErrorMessage("Can't download the design")
                     }
                } else {
                    console.log("Can't download the design: status = " + response.status)
                    onShowErrorMessage("Can't download the design")
                }
            })
            .catch(function (error) {
                console.log("Can't download the design: " + error)
                onShowErrorMessage("Can't download the design")
            })
    }

    const onUpdate = (e) => {
        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        const newDesign = { manifest: design.manifest, metadata: design.metadata, script: design.script, published: design.published }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/validate', newDesign, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        axios.put(config.api_url + '/v1/designs/' + uuid, newDesign, axiosConfig)
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
                        console.log("Can't update the design: statue " + result.status)
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

    const onPublish = (e) => {
        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        const newDesign = { manifest: design.manifest, metadata: design.metadata, script: design.script, published: true }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/validate', newDesign, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        axios.put(config.api_url + '/v1/designs/' + uuid, newDesign, axiosConfig)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    setDesign({...design, published: true})
                                    onShowErrorMessage("Your request has been received. The design will be updated shortly")
                                } else {
                                    console.log("Can't publish the design: status = " + response.status)
                                    onShowErrorMessage("Can't publish the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't publish the design: " + error)
                                onShowErrorMessage("Can't publish the design")
                            })
                     } else {
                        console.log("Can't publish the design: " + result.status)
                        onShowErrorMessage("Can't publish the design")
                     }
                } else {
                    console.log("Can't publish the design: status = " + response.status)
                    onShowErrorMessage("Can't publish the design")
                }
            })
            .catch(function (error) {
                console.log("Can't publish the design: " + error)
                onShowErrorMessage("Can't publish the design")
            })
    }

    const onUnpublish = (e) => {
        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        const newDesign = { manifest: design.manifest, metadata: design.metadata, script: design.script, published: false }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/validate', newDesign, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        axios.put(config.api_url + '/v1/designs/' + uuid, newDesign, axiosConfig)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    setDesign({...design, published: false})
                                    onShowErrorMessage("Your request has been received. The design will be updated shortly")
                                } else {
                                    console.log("Can't unpublish the design: status = " + response.status)
                                    onShowErrorMessage("Can't unpublish the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't unpublish the design: " + error)
                                onShowErrorMessage("Can't unpublish the design")
                            })
                     } else {
                        console.log("Can't unpublish the design: " + result.status)
                        onShowErrorMessage("Can't unpublish the design")
                     }
                } else {
                    console.log("Can't unpublish the design: status = " + response.status)
                    onShowErrorMessage("Can't unpublish the design")
                }
            })
            .catch(function (error) {
                console.log("Can't unpublish the design: " + error)
                onShowErrorMessage("Can't unpublish the design")
            })
    }

    const onEditorChanged = (value) => {
        setDesign({...design, script: value.script, metadata: value.metadata})
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
