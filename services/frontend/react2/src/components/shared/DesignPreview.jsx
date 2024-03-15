import React, { useEffect, useState, useCallback } from "react"
import usePreview from '../../hooks/usePreview'

import Stack from '@mui/material/Stack'
import Grid from '@mui/material/Grid'
import DesignEditor from '../shared/DesignEditor'

export default function DesignPreview({ initialDesign, onPreviewChanged }) {
    const [ design, setDesign ] = useState(initialDesign)
    const [ message, setMessage ] = useState("Initializing...")
    const [ imageUrl, setImageUrl ] = useState(null)

    const onLoadPreview = useCallback((message) => {
        setMessage(message)
    }, [setMessage])

    const onLoadPreviewSuccess = useCallback((message, imageUrl) => {
        setMessage(message)
        setImageUrl(imageUrl)
    }, [setMessage, setImageUrl])

    const onLoadPreviewFailure = useCallback((message) => {
        setMessage(message)
    }, [setMessage])

    usePreview({
        design: design,
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
                <DesignEditor initialDesign={design} onEditorChanged={onEditorChanged}/>
            </Grid>
        </Grid>
    )
}
