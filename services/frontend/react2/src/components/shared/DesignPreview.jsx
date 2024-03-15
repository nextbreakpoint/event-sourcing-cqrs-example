import React, { useEffect, useState, useCallback } from "react"
import { useSelector, useDispatch } from 'react-redux'
import usePreview from '../../hooks/usePreview'

import Stack from '@mui/material/Stack'
import Grid from '@mui/material/Grid'
import DesignEditor from '../shared/DesignEditor'

import {
    getConfig
} from '../../actions/config'

import axios from 'axios'

export default function DesignPreview({ script, metadata, manifest, onPreviewChanged }) {
    const [ design, setDesign ] = useState({script: script, metadata: metadata, manifest: manifest})
    const [ message, setMessage ] = useState("Initializing...")
    const [ imageUrl, setImageUrl ] = useState(null)
    const config = useSelector(getConfig)

    const onLoadPreview = useCallback((message) => {
        setMessage(message)
    }, [setMessage])

    const onLoadPreviewSuccess = useCallback((message, checksum, time) => {
        setMessage(message)
        setImageUrl(config.api_url + '/v1/designs/image/' + checksum + '?t=' + time)
    }, [setMessage, setImageUrl])

    const onLoadPreviewFailure = useCallback((message) => {
        setMessage(message)
    }, [setMessage])

    usePreview({
        design: design,
        appConfig: config,
        onLoadPreview: onLoadPreview,
        onLoadPreviewSuccess: onLoadPreviewSuccess,
        onLoadPreviewFailure: onLoadPreviewFailure
    })

    const onEditorChanged = useCallback((editorState) => {
        setDesign({...design, script: editorState.script, metadata: editorState.metadata})
        onPreviewChanged({...design, script: editorState.script, metadata: editorState.metadata})
    }, [design, setDesign])

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
                <DesignEditor script={design.script} metadata={design.metadata} manifest={design.manifest} onEditorChanged={onEditorChanged}/>
            </Grid>
        </Grid>
    )
}
