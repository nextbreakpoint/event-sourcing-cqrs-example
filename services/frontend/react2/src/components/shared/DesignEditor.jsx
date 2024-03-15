import React from 'react'
import { useState, useCallback } from 'react';

import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Typography from '@mui/material/Typography'
import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'

export default function DesignEditor({ initialDesign, onEditorChanged }) {
    const [ design, setDesign ] = useState(initialDesign)

    const onScriptChanged = useCallback((script) => {
        setDesign({...design, script: script})
        if (onEditorChanged) {
            onEditorChanged({...design, script: script})
        }
    }, [design, setDesign, onEditorChanged])

    const onMetadataChanged = useCallback((metadata) => {
        setDesign({...design, metadata: metadata})
        if (onEditorChanged) {
            onEditorChanged({...design, metadata: metadata})
        }
    }, [design, setDesign, onEditorChanged])

    return (
        <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
            <Grid item xs={7}>
                <div class="form">
                    <div><Typography variant="body" color="inherit" class="form-label">Script</Typography></div>
                    <ScriptEditor initialValue={design.script} readOnly={false} onContentChanged={onScriptChanged}/>
                </div>
            </Grid>
            <Grid item xs={5}>
                <div class="form">
                    <div><Typography variant="body" color="inherit" class="form-label">Metadata</Typography></div>
                    <MetadataEditor initialValue={design.metadata} readOnly={false} onContentChanged={onMetadataChanged}/>
                </div>
            </Grid>
        </Grid>
    )
}
