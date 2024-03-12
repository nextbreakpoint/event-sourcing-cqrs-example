import React from 'react'
import { useState, useCallback } from 'react';
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

    const onLoadConfigCallback = useCallback(() => dispatch(loadConfig()), [dispatch])
    const onLoadConfigSuccessCallback = useCallback((config) => dispatch(loadConfigSuccess(config)), [dispatch])
    const onLoadConfigFailureCallback = useCallback((error) => dispatch(loadConfigFailure(error)), [dispatch])

    useConfig({
        onLoadConfig: onLoadConfigCallback,
        onLoadConfigSuccess: onLoadConfigSuccessCallback,
        onLoadConfigFailure: onLoadConfigFailureCallback
    })

    return (
        config ? props.children : <div><Message error={status.error} text={status.message}/></div>
    )
}
