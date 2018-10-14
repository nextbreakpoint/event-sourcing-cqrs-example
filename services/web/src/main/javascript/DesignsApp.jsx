import React from 'react'
import ReactDOM from 'react-dom'
import PropTypes from 'prop-types'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Header from './Header'
import Footer from './Footer'
import Config from './Config'
import Account from './Account'
import Designs from './Designs'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Grid from '@material-ui/core/Grid'

import { connect } from 'react-redux'

import { setAccount } from './actions/designs'

import axios from 'axios'

import Cookies from 'universal-cookie'

const cookies = new Cookies()

const base_url = 'https://localhost:8080'

const store = createStore(reducers)

const styles = theme => ({
  button: {
    position: 'absolute',
    bottom: theme.spacing.unit * 2,
    right: theme.spacing.unit * 2
  }
})

class App extends React.Component {
    handleLogin = () => {
        window.location = this.props.config.auth_url + "/signin/admin/designs"
    }

    handleLogout = () => {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})

        this.props.handleAccountLoaded({ role: 'anonymous', name: 'Stranger' })
    }

    render() {
        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header role={this.props.account.role} name={this.props.account.name} onLogin={this.handleLogin} onLogout={this.handleLogout}/>
                    </Grid>
                    <Grid item xs={12}>
                        <Designs/>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer role={this.props.account.role} name={this.props.account.name}/>
                    </Grid>
                </Grid>
            </React.Fragment>
        )
    }
}

const mapStateToProps = state => {
    console.log(JSON.stringify(state))

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

const DesignsApp = withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(App))

ReactDOM.render(<Provider store={store}><Config><Account><DesignsApp /></Account></Config></Provider>, document.querySelector('#app'))
