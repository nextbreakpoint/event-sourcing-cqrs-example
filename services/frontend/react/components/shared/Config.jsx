import React from 'react'
import PropTypes from 'prop-types'

import Grid from '@material-ui/core/Grid'

import Message from './Message'

import { connect } from 'react-redux'

import {
    getConfig,
    getConfigStatus,
    loadConfig,
    loadConfigSuccess,
    loadConfigFailure
} from '../../actions/config'

import axios from 'axios'

class Config extends React.Component {
    componentDidMount = () => {
        this.props.handleLoadConfigSuccess(window.config)
    }

    render() {
        return (
            this.props.config ? (this.props.children) : (
                <div>
                    <Message error={this.props.status.error} text={this.props.status.message}/>
                </div>
            )
        )
    }
}

const mapStateToProps = state => ({
    config: getConfig(state),
    status: getConfigStatus(state)
})

const mapDispatchToProps = dispatch => ({
    handleLoadConfig: () => {
        dispatch(loadConfig())
    },
    handleLoadConfigSuccess: (config) => {
        dispatch(loadConfigSuccess(config))
    },
    handleLoadConfigFailure: (error) => {
        dispatch(loadConfigFailure(error))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Config)

