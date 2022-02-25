import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import Link from '@material-ui/core/Link';
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import MenuIcon from '@material-ui/icons/Menu';

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
    state = {
        anchorEl: null
    }

    handleNavigateContentDesigns = () => {
        window.location = this.props.config.web_url + "/browse/designs.html"
    }

    handleNavigateAdminDesigns = () => {
        window.location = this.props.config.web_url + "/admin/designs.html"
    }

    handleLogin = () => {
        window.location = this.props.config.api_url + "/v1/auth/signin" + this.props.landing
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
        const { classes, titleLink, titleText, titleText2, browseLink, browseText } = this.props
        const { anchorEl } = this.state
        const open = Boolean(anchorEl)

        return (
            <AppBar position="static">
                <Toolbar>
                  <Typography variant="title" color="inherit" className={classes.grow}>{browseLink != null && <span><Link href={browseLink}>{browseText}</Link></span>}</Typography>
                  <Typography variant="title" color="inherit" className={classes.grow}>{titleLink != null && <span><Link href={titleLink}>{titleText}</Link>{titleText2 != null && <span> | {titleText2}</span>}</span>}{titleLink == null && <span>{titleText}{titleText2 != null && <span> | {titleText2}</span>}</span>}</Typography>
                  {this.props.account.role == 'anonymous' && <Button color="inherit" onClick={this.handleLogin}>Login</Button>}
                  {this.props.account.role != 'anonymous' && <Button color="inherit" onClick={this.handleLogout}>Logout</Button>}
                </Toolbar>
            </AppBar>
        )
    }
}

//                   <IconButton className={classes.menuButton} color="inherit" aria-label="Menu">
//                     <MenuIcon onClick={this.handleMenu}/>
//                   </IconButton>
//                   <Menu
//                     id="menu-appbar"
//                     anchorEl={anchorEl}
//                     anchorOrigin={{
//                       vertical: 'top',
//                       horizontal: 'right',
//                     }}
//                     transformOrigin={{
//                       vertical: 'top',
//                       horizontal: 'right',
//                     }}
//                     open={open}
//                     onClose={this.handleClose}
//                   >
//                     <MenuItem onClick={this.handleNavigateContentDesigns}>Browse</MenuItem>
//                     <MenuItem onClick={this.handleNavigateAdminDesigns}>Admin</MenuItem>
//                   </Menu>


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

Header.propTypes = {
    landing: PropTypes.string.isRequired,
    titleLink: PropTypes.string.isRequired,
    titleText: PropTypes.string.isRequired,
    titleText2: PropTypes.string.isRequired,
    browseLink: PropTypes.string.isRequired,
    browseText: PropTypes.string.isRequired,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired
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

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Header))
