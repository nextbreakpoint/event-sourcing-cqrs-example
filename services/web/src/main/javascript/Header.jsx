import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import MenuIcon from '@material-ui/icons/Menu';

import { connect } from 'react-redux'

import { setAccount } from './actions/designs'

import axios from 'axios'

import Cookies from 'universal-cookie'

const cookies = new Cookies()

var uuid = undefined

const regexp = /https?:\/\/.*\/admin\/designs\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/g
const match = regexp.exec(window.location.href)

if (match != null && match.length == 2) {
    uuid = match[1]
}

const base_url = 'https://localhost:8080'

const styles = theme => ({
  root: {
    flexGrow: 1
  },
  grow: {
    flexGrow: 1
  },
  button: {
    padding: 0
  },
  menuButton: {
    marginRight: 2 * theme.spacing.unit
  }
})

class Header extends React.Component {
    state = {
        anchorEl: null
    }

    handleNavigateContentDesigns = () => {
        window.location = base_url + "/content/designs"
    }

    handleNavigateAdminDesigns = () => {
        window.location = base_url + "/admin/designs"
    }

    handleLogin = () => {
        window.location = this.props.config.auth_url + "/signin" + this.props.landing
    }

    handleLogout = () => {
        cookies.remove('token', {domain: window.location.hostname, path: '/'})

        this.props.handleAccountLoaded({ role: 'anonymous', name: 'Stranger' })
    }

    handleMenu = event => {
        this.setState({ anchorEl: event.currentTarget })
    }

    handleClose = () => {
        this.setState({ anchorEl: null })
    }

    render() {
        const { classes } = this.props
        const { anchorEl } = this.state
        const open = Boolean(anchorEl)

        return (
            <AppBar position="static">
                <Toolbar>
                  <IconButton className={classes.menuButton} color="inherit" aria-label="Menu">
                    <MenuIcon onClick={this.handleMenu}/>
                  </IconButton>
                  <Menu
                    id="menu-appbar"
                    anchorEl={anchorEl}
                    anchorOrigin={{
                      vertical: 'top',
                      horizontal: 'right',
                    }}
                    transformOrigin={{
                      vertical: 'top',
                      horizontal: 'right',
                    }}
                    open={open}
                    onClose={this.handleClose}
                  >
                    <MenuItem onClick={this.handleNavigateContentDesigns}>Browse</MenuItem>
                    <MenuItem onClick={this.handleNavigateAdminDesigns}>Admin</MenuItem>
                  </Menu>
                  <Typography variant="title" color="inherit" className={classes.grow}>
                    Designs {uuid && (<span>| {uuid}</span>)}
                  </Typography>
                  {this.props.account.role == 'anonymous' && <Button color="inherit" onClick={this.handleLogin}>Login</Button>}
                  {this.props.account.role != 'anonymous' && <Button color="inherit" onClick={this.handleLogout}>Logout</Button>}
                </Toolbar>
            </AppBar>
        )
    }
}

Header.propTypes = {
  landing: PropTypes.string.isRequired,
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
