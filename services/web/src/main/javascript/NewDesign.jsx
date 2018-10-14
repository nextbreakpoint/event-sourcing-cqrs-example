import React from 'react'
import PropTypes from 'prop-types'

import Button from '@material-ui/core/Button'
import Grid from '@material-ui/core/Grid'

import ScriptEditor from './ScriptEditor'
import MetadataEditor from './MetadataEditor'

let NewDesign = class NewDesign extends React.Component {
    constructor(props) {
        super(props)

        this.scriptEditor = React.createRef()
        this.metadataEditor = React.createRef()

        this.state = { script: props.script, metadata: props.metadata }

        this.handleScriptChanged = this.handleScriptChanged.bind(this)
        this.handleMetadataChanged = this.handleMetadataChanged.bind(this)
    }

    handleScriptChanged(value) {
        this.setState({script: value, metadata: this.state.metadata})
        this.props.onScriptChanged(this.state.script)
    }

    handleMetadataChanged(value) {
        this.setState({script: this.state.script, metadata: value})
        this.props.onMetadataChanged(this.state.metadata)
    }

    render() {
        return (
            <Grid container justify="space-between" alignItems="stretch" alignContent="space-between">
                <Grid item xs={6}>
                    <p>Script</p>
                    <ScriptEditor ref={this.scriptEditor} initialValue={this.state.script} readOnly={false} onContentChanged={(value) => this.handleScriptChanged(value)}/>
                </Grid>
                <Grid item xs={5}>
                    <p>Metadata</p>
                    <MetadataEditor ref={this.metadataEditor} initialValue={this.state.metadata} readOnly={false} onContentChanged={(value) => this.handleScriptChanged(value)}/>
                </Grid>
            </Grid>
        )
    }
}

NewDesign.propTypes = {
  script: PropTypes.string.isRequired,
  metadata: PropTypes.string.isRequired,
  onScriptChanged: PropTypes.func.isRequired,
  onMetadataChanged: PropTypes.func.isRequired
}

export default NewDesign
