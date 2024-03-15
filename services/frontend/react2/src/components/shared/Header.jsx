import React from 'react'
import { useRef, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import useAccount from '../../hooks/useAccount'

import Cookies from 'universal-cookie'

import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';

import {
    getConfig
} from '../../actions/config'

import {
    getAccount,
    loadAccountSuccess
} from '../../actions/account'

export default function Header({ landing, titleText, subtitleText, backText, backLink, browseText, browseLink }) {
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const dispatch = useDispatch()

    const cookiesRef = useRef(new Cookies())

    const handleBack = useCallback(() => {
        window.location = backLink
    }, [backLink])

    const handleBrowse = useCallback(() => {
        window.location = browseLink
    }, [browseLink])

    const handleLogin = useCallback(() => {
        window.location = config.api_url + "/v1/auth/signin" + landing
    }, [config, landing])

    const handleLogout = useCallback(() => {
        cookiesRef.current.remove('token', {domain: window.location.hostname, path: '/'})
        dispatch(loadAccountSuccess({ role: 'anonymous', name: 'Stranger' }))
    }, [dispatch])

    return (
        <AppBar position="static">
            <Toolbar className="header">
              <div class="grow">
                  <Typography variant="title" color="inherit"><span class="title">{titleText}</span><span class="separator">|</span><span class="subtitle">{subtitleText}</span></Typography>
              </div>
              <navigation class="grow">
                  {account.role != 'anonymous' && <Typography variant="button" color="inherit" className="account">Welcome {account.name},</Typography>}
                  {browseLink != null && <Button color="inherit" variant="text" onClick={handleBrowse}>{browseText}</Button>}
                  {backLink != null && <Button color="inherit" variant="text" onClick={handleBack}>{backText}</Button>}
                  {account.role == 'anonymous' && <Button color="inherit" variant="text" onClick={handleLogin}>Login</Button>}
                  {account.role != 'anonymous' && <Button color="inherit" variant="text" onClick={handleLogout}>Logout</Button>}
              </navigation>
            </Toolbar>
        </AppBar>
    )
}
