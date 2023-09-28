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
        this.setState({script: value})
        this.props.onScriptChanged(value)
    }

    handleMetadataChanged = (value) => {
        this.setState({metadata: value})
        this.props.onMetadataChanged(value)
    }

    render() {
        return (
            <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
                <Grid item xs={7} className="script">
                    <ScriptEditor initialValue={this.state.script} readOnly={false} onContentChanged={this.handleScriptChanged}/>
                </Grid>
                <Grid item xs={5} className="metadata">
                    <MetadataEditor initialValue={this.state.metadata} readOnly={false} onContentChanged={this.handleMetadataChanged}/>
                </Grid>
            </Grid>
        )
    }
}

const styles = {}

DesignForm.propTypes = {
  script: PropTypes.string.isRequired,
  metadata: PropTypes.string.isRequired,
  onScriptChanged: PropTypes.func.isRequired,
  onMetadataChanged: PropTypes.func.isRequired
}

export default DesignForm
