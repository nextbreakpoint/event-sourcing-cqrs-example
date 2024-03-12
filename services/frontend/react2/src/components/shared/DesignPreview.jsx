import React, { useEffect, useState, useCallback } from "react"
import PropTypes from 'prop-types'

import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Grid from '@mui/material/Grid'

import { MapContainer, TileLayer, useMap } from 'react-leaflet'

import DesignEditor from '../shared/DesignEditor'

import axios from 'axios'

export default function DesignPreview(props) {
    const [script, setScript] = useState(props.script)
    const [metadata, setMetadata] = useState(props.metadata)
    const [manifest, setManifest] = useState(props.manifest)
    const [checksum, setChecksum] = useState(null)
    const [imageUrl, setImageUrl] = useState(null)
    const [message, setMessage] = useState("Initializing...")

    useEffect(() => {
        const timeout = setTimeout(() => {
            handleRender(createDesign(manifest, script, metadata))
        }, 2000)

        return () => {
            clearTimeout(timeout)
        }
      }, [script, metadata])

    const createDesign = (manifest, script, metadata) => {
        return { manifest: manifest, script: script, metadata: metadata }
    }

    const handleRender = (design) => {
        console.log("render")

        const config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        setMessage("Rendering...")

        axios.post(props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.post(props.config.api_url + '/v1/designs/render', design, config)
                            .then(function (response) {
                                if (response.status == 200) {
                                    console.log("Checksum = " + response.data.checksum)
                                    const date = new Date()
                                    setChecksum(response.data.checksum)
                                    setImageUrl(props.config.api_url + '/v1/designs/image/' + response.data.checksum + '?t=' + date.getTime())
                                    setMessage("Last updated " + date.toISOString())
                                } else {
                                    console.log("Can't render the design: status = " + response.status)
                                    setMessage("Can't render the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't render the design: " + error)
                                setMessage("Can't render the design")
                            })
                     } else {
                        const errors = result.errors.join(', ');
                        console.log("The design contains some errors: " + errors)
                        setMessage("The design contains some errors: " + errors)
                     }
                } else {
                    console.log("Can't validate the design: status = " + response.status)
                    setMessage("Can't validate the design")
                }
            })
            .catch(function (error) {
                console.log("Can't validate the design: " + error)
                setMessage("Can't validate the design")
            })
    }

    const handleEditorChanged = (setScript, setMetadata, design) => {
        setScript(design.script)
        setMetadata(design.metadata)
        props.onEditorChanged(design)
    }

    return (
        <Grid container justify="space-between" alignItems="stretch" alignContent="space-between" className="design-editor">
            <Grid item xs={6}>
                <Stack direction="column" alignItems="center" justifyContent="space-between">
                  <div className="editor-preview">
                      {imageUrl != null && (<img src={imageUrl}/>)}
                  </div>
                  <div className="editor-message">{message}</div>
                </Stack>
            </Grid>
            <Grid item xs={6}>
                <DesignEditor script={script} metadata={metadata} manifest={manifest} onEditorChanged={design => handleEditorChanged(setScript, setMetadata, design)}/>
            </Grid>
        </Grid>
    )
}
