import React from 'react'
import { useSelector, useDispatch } from 'react-redux'
import useConfig from '../../hooks/useConfig'

import Grid from '@mui/material/Grid'
import Message from './Message'

import {
    getConfig,
    getConfigStatus,
    loadConfig,
    loadConfigSuccess,
    loadConfigFailure
} from '../../actions/config'

export default function Config(props) {
    const config = useSelector(getConfig)
    const status = useSelector(getConfigStatus)
    const dispatch = useDispatch()

    useConfig({
        onLoadConfig: () => dispatch(loadConfig()),
        onLoadConfigSuccess: (account) => dispatch(loadConfigSuccess(account)),
        onLoadConfigFailure: (error) => dispatch(loadConfigFailure(error))
    })

    return (
        config ? props.children : <div><Message error={status.error} text={status.message}/></div>
    )
}
