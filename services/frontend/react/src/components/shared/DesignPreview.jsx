import React, { useEffect, useState, useCallback } from "react"
import PropTypes from 'prop-types'

import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Grid from '@mui/material/Grid'

import { MapContainer, TileLayer, useMap } from 'react-leaflet'

import DesignForm from '../shared/DesignForm'

import axios from 'axios'

let manifest = "{\"pluginId\":\"Mandelbrot\"}"

export default function DesignPreview(props) {
    let [script, setScript] = useState(props.script)
    let [metadata, setMetadata] = useState(props.metadata)
    let [checksum, setChecksum] = useState(null)
    let [imageUrl, setImageUrl] = useState(null)
    let [message, setMessage] = useState("Initializing...")

    useEffect(() => {
        let timeout = setTimeout(() => {
            handleRender(createDesign(manifest, script, metadata))
        }, 2000)

        return () => {
            clearTimeout(timeout)
        }
      }, [script, metadata])

    let createDesign = (manifest, script, metadata) => {
        return { manifest: manifest, script: script, metadata: metadata }
    }

    let handleRender = (design) => {
        console.log("render")

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        setMessage("Rendering...")

        axios.post(props.config.api_url + '/v1/designs/validate', design, config)
            .then(function (response) {
                if (response.status == 200) {
                     let result = response.data
                     console.log(result)
                     if (result.status == "ACCEPTED") {
                        axios.post(props.config.api_url + '/v1/designs/render', design, config)
                            .then(function (response) {
                                if (response.status == 200) {
                                    console.log("Checksum = " + response.data.checksum)
                                    let date = new Date()
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
                        let errors = result.errors.join(', ');
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

    let handleScriptChanged = (setScript, value) => {
        setScript(value)
        props.onScriptChanged(value)
    }

    let handleMetadataChanged = (setMetadata, value) => {
        setMetadata(value)
        props.onMetadataChanged(value)
    }

    return (
        <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
            <Grid item xs={6}>
                <Stack direction="column" alignItems="center" justifyContent="space-between">
                  <div className="image-preview">
                      {imageUrl != null && (<img src={imageUrl}/>)}
                  </div>
                  <div className="message">{message}</div>
                </Stack>
            </Grid>
            <Grid item xs={6}>
                <DesignForm script={script} metadata={metadata} onScriptChanged={e => handleScriptChanged(setScript, e)} onMetadataChanged={e => handleMetadataChanged(setMetadata, e)}/>
            </Grid>
        </Grid>
    )
}

