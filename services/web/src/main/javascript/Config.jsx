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
} from './actions/designs'

import axios from 'axios'

class Config extends React.Component {
    componentDidMount = () => {
        console.log("Loading config...")

        let component = this

        let config = {
            timeout: 5000,
            withCredentials: true
        }

        component.props.handleLoadConfig()

        axios.get('/config', config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Config loaded")
                    let config = response.data
                    component.props.handleLoadConfigSuccess(config)
                } else {
                    console.log("Can't load config: status = " + response.status)
                    component.props.handleLoadConfigFailure("Can't load config")
                }
            })
            .catch(function (error) {
                console.log("Can't load config: " + error)
                component.props.handleLoadConfigFailure("Can't load config")
            })
    }

    render() {
        return (
            this.props.config ? (this.props.children) : (<Message error={this.props.status.error} text={this.props.status.message}/>)
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

