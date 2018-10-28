import React from 'react'
import PropTypes from 'prop-types'

import Grid from '@material-ui/core/Grid'

import Message from './Message'

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount,
    getAccountStatus,
    loadAccount,
    loadAccountSuccess,
    loadAccountFailure
} from '../../actions/account'

import Cookies from 'universal-cookie'

import axios from 'axios'

const cookies = new Cookies()

class Account extends React.Component {
    componentDidMount = () => {
        console.log("Loading account...")

        let component = this

        let config = {
            timeout: 5000,
            withCredentials: true
        }

        component.props.handleLoadAccount()

        axios.get(component.props.config.api_url + '/accounts/me', config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Account loaded")
                    let { role, name } = response.data
                    component.props.handleLoadAccountSuccess({ role, name })
                } else if (response.status == 403) {
                    console.log("Not authenticated")
                    cookies.remove('token', {domain: window.location.hostname})
                    component.props.handleLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
                } else {
                    console.log("Can't load account: status = " + response.status)
                    cookies.remove('token', {domain: window.location.hostname})
                    component.props.handleLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
                }
            })
            .catch(function (error) {
                console.log("Can't load account: " + error)
                cookies.remove('token', {domain: window.location.hostname})
                component.props.handleLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
            })
    }

    render() {
        return (
            this.props.account ? (this.props.children) : (<Message error={this.props.status.error} text={this.props.status.message}/>)
        )
    }
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    status: getAccountStatus(state)
})

const mapDispatchToProps = dispatch => ({
    handleLoadAccount: () => {
        dispatch(loadAccount())
    },
    handleLoadAccountSuccess: (config) => {
        dispatch(loadAccountSuccess(config))
    },
    handleLoadAccountFailure: (error) => {
        dispatch(loadAccountFailure(error))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Account)
