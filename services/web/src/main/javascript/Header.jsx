import React from 'react'
import PropTypes from 'prop-types'

import Grid from '@material-ui/core/Grid'

import { withStyles } from '@material-ui/core/styles'

import { connect } from 'react-redux'

import { setAccount } from './actions/designs'

import axios from 'axios'

import Cookies from 'universal-cookie'

const cookies = new Cookies()

const styles = theme => ({
})

class Header extends React.Component {
    handleLogin = () => {
        window.location = this.props.config.auth_url + "/signin/admin/designs"
    }

    handleLogout = () => {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})

        this.props.handleAccountLoaded({ role: 'anonymous', name: 'Stranger' })
    }

    render() {
        return (
            <header>
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={6}>
                        <span>Welcome {this.props.account.name}</span>
                    </Grid>
                    <Grid item xs={6} className="right-align">
                        {this.props.parent && <span><a href={this.props.parent.link}>{this.props.parent.label}</a> | </span>}
                        {this.props.account.role == 'anonymous' && <span onClick={this.handleLogin}>Login</span>}
                        {this.props.account.role != 'anonymous' && <span onClick={this.handleLogout}>Logout</span>}
                    </Grid>
                </Grid>
            </header>
        )
    }
}

Header.propTypes = {
  parent: PropTypes.object
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

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Header))
