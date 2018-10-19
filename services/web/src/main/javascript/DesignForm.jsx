import React from 'react'
import PropTypes from 'prop-types'

import Button from '@material-ui/core/Button'
import Grid from '@material-ui/core/Grid'

import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'

let DesignForm = class DesignForm extends React.Component {
    state = {
        script: this.props.script,
        metadata: this.props.metadata
    }

    handleScriptChanged = (value) => {
        this.setState({script: value, metadata: this.state.metadata})
        this.props.onScriptChanged(this.state.script)
    }

    handleMetadataChanged = (value) => {
        this.setState({script: this.state.script, metadata: value})
        this.props.onMetadataChanged(this.state.metadata)
    }

    render() {
        return (
            <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
                <Grid item xs={6}>
                    <p>Script</p>
                    <ScriptEditor initialValue={this.state.script} readOnly={false} onContentChanged={(value) => this.handleScriptChanged(value)}/>
                </Grid>
                <Grid item xs={5}>
                    <p>Metadata</p>
                    <MetadataEditor initialValue={this.state.metadata} readOnly={false} onContentChanged={(value) => this.handleMetadataChanged(value)}/>
                </Grid>
            </Grid>
        )
    }
}

DesignForm.propTypes = {
  script: PropTypes.string.isRequired,
  metadata: PropTypes.string.isRequired,
  onScriptChanged: PropTypes.func.isRequired,
  onMetadataChanged: PropTypes.func.isRequired
}

export default DesignForm
