import React from 'react'
import PropTypes from 'prop-types'

import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount,
    loadAccountSuccess
} from '../../actions/account'

import axios from 'axios'

import Cookies from 'universal-cookie'

const cookies = new Cookies()

let Header = class Header extends React.Component {
    handleBack = () => {
        window.location = this.props.backLink
    }

    handleBrowse = () => {
        window.location = this.props.browseLink
    }

    handleLogin = () => {
        window.location = this.props.config.api_url + "/v1/auth/signin" + this.props.landing
    }

    handleLogout = () => {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})
        this.props.handleAccountLoaded({ role: 'anonymous', name: 'Stranger' })
    }

    render() {
        const { titleText, subtitleText, backText, backLink, browseText, browseLink, account } = this.props

        return (
            <AppBar position="static">
                <Toolbar className="header">
                  <div class="grow">
                      <Typography variant="title" color="inherit"><span class="title">{titleText}</span><span class="separator">|</span><span class="subtitle">{subtitleText}</span></Typography>
                  </div>
                  <navigation class="grow">
                      {account.role != 'anonymous' && <Typography variant="button" color="inherit" className="account">Welcome {account.name},</Typography>}
                      {browseLink != null && <Button color="inherit" variant="text" onClick={this.handleBrowse}>{browseText}</Button>}
                      {backLink != null && <Button color="inherit" variant="text" onClick={this.handleBack}>{backText}</Button>}
                      {account.role == 'anonymous' && <Button color="inherit" variant="text" onClick={this.handleLogin}>Login</Button>}
                      {account.role != 'anonymous' && <Button color="inherit" variant="text" onClick={this.handleLogout}>Logout</Button>}
                  </navigation>
                </Toolbar>
            </AppBar>
        )
    }
}

Header.propTypes = {
    landing: PropTypes.string.isRequired,
    titleLink: PropTypes.string.isRequired,
    subtitleText: PropTypes.string.isRequired,
    backLink: PropTypes.string.isRequired,
    backText: PropTypes.string.isRequired,
    browseLink: PropTypes.string.isRequired,
    browseText: PropTypes.string.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state)
})

const mapDispatchToProps = dispatch => ({
    handleAccountLoaded: (account) => {
        dispatch(loadAccountSuccess(account))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Header)
