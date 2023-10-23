import React from 'react'
import PropTypes from 'prop-types'

import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import MenuIcon from '@mui/icons-material/Menu';

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
        const { titleLink, titleText, titleText2, browseLink, browseText } = this.props
        const { anchorEl } = this.state
        const open = Boolean(anchorEl)

        return (
            <AppBar position="static">
                <Toolbar className="header">
                  <Typography variant="title" color="inherit" className="grow">{browseLink != null && <span className="browse"><Link href={browseLink}>{browseText}</Link></span>} | {titleLink != null && <span><Link href={titleLink}>{titleText}</Link>{titleText2 != null && <span> | {titleText2}</span>}</span>}{titleLink == null && <span>{titleText}{titleText2 != null && <span> | {titleText2}</span>}</span>}</Typography>
                  {this.props.account.role == 'anonymous' && <Button color="inherit" onClick={this.handleLogin}>Login</Button>}
                  {this.props.account.role != 'anonymous' && <Button color="inherit" onClick={this.handleLogout}>Logout</Button>}
                </Toolbar>
            </AppBar>
        )
    }
}

//                   <IconButton className={"menu-button"} color="inherit" aria-label="Menu">
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

Header.propTypes = {
    landing: PropTypes.string.isRequired,
    titleLink: PropTypes.string.isRequired,
    titleText: PropTypes.string.isRequired,
    titleText2: PropTypes.string.isRequired,
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
