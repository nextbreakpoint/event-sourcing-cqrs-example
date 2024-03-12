import React from 'react'
import PropTypes from 'prop-types'

import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Typography from '@mui/material/Typography'

import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'

let DesignEditor = class DesignEditor extends React.Component {
    state = {
        script: this.props.script,
        metadata: this.props.metadata,
        manifest: this.props.manifest
    }

    handleScriptChanged = (script) => {
        this.setState({script: script})
        this.props.onEditorChanged({...this.state, script: script})
    }

    handleMetadataChanged = (metadata) => {
        this.setState({metadata: metadata})
        this.props.onEditorChanged({...this.state, metadata: metadata})
    }

    render() {
        return (
            <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
                <Grid item xs={7}>
                    <div class="form">
                        <div><Typography variant="body" color="inherit" class="form-label">Script</Typography></div>
                        <ScriptEditor initialValue={this.state.script} readOnly={false} onContentChanged={this.handleScriptChanged}/>
                    </div>
                </Grid>
                <Grid item xs={5}>
                    <div class="form">
                        <div><Typography variant="body" color="inherit" class="form-label">Metadata</Typography></div>
                        <MetadataEditor initialValue={this.state.metadata} readOnly={false} onContentChanged={this.handleMetadataChanged}/>
                    </div>
                </Grid>
            </Grid>
        )
    }
}

DesignEditor.propTypes = {
  script: PropTypes.string.isRequired,
  metadata: PropTypes.string.isRequired,
  manifest: PropTypes.string.isRequired,
  onEditorChanged: PropTypes.func.isRequired
}

export default DesignEditor
