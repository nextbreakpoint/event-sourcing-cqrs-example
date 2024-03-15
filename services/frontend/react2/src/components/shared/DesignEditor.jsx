import React from 'react'
import { useState, useCallback } from 'react';

import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Typography from '@mui/material/Typography'
import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'

export default function DesignEditor({ script, metadata, manifest, onEditorChanged }) {
    const [ editorState, setEditorState ] = useState({script: script, metadata: metadata, manifest: manifest})

    const onScriptChanged = useCallback((script) => {
        setEditorState({...editorState, script: script})
        onEditorChanged({...editorState, script: script})
    }, [editorState, setEditorState, onEditorChanged])

    const onMetadataChanged = useCallback((metadata) => {
        setEditorState({...editorState, metadata: metadata})
        onEditorChanged({...editorState, metadata: metadata})
    }, [editorState, setEditorState, onEditorChanged])

    return (
        <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
            <Grid item xs={7}>
                <div class="form">
                    <div><Typography variant="body" color="inherit" class="form-label">Script</Typography></div>
                    <ScriptEditor initialValue={script} readOnly={false} onContentChanged={onScriptChanged}/>
                </div>
            </Grid>
            <Grid item xs={5}>
                <div class="form">
                    <div><Typography variant="body" color="inherit" class="form-label">Metadata</Typography></div>
                    <MetadataEditor initialValue={metadata} readOnly={false} onContentChanged={onMetadataChanged}/>
                </div>
            </Grid>
        </Grid>
    )
}
