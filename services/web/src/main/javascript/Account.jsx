import React from 'react'
import ReactDOM from 'react-dom'
import PropTypes from 'prop-types'

import reducers from './reducers'

import { connect } from 'react-redux'

import { setAccount } from './actions/designs'

import Cookies from 'universal-cookie'

import axios from 'axios'

const base_url = 'https://localhost:8080'

const cookies = new Cookies()

class Account extends React.Component {
    componentDidMount = () => {
        console.log("Loading account...")

        let component = this

        let config = {
            timeout: 5000,
            withCredentials: true
        }

        axios.get(component.props.config.accounts_url + '/me', config)
            .then(function (content) {
                console.log("Account loaded")

                let { role, name } = content.data

                component.props.handleAccountLoaded({ role, name })
            })
            .catch(function (error) {
                console.log("Account loaded (are you logged in?)")

                cookies.remove('token', {domain: window.location.hostname})

                component.props.handleAccountLoaded({ role: 'anonymous', name: 'Stranger' })
            })
    }

    render() {
        let children = this.props.children

        if (this.props.account) {
            return (
                <div>{children}</div>
            )
        } else {
            return (
                <div><p>Loading account...</p></div>
            )
        }
    }
}

const mapStateToProps = state => {
    //console.log(JSON.stringify(state))

    return {
        config: state.designs.config,
        account: state.designs.account
    }
}

const mapDispatchToProps = dispatch => ({
    handleAccountLoaded: (account) => {
        dispatch(setAccount(account))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Account)

