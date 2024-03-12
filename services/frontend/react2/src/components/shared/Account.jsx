import React from 'react'
import { useState, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux'
import useAccount from '../../hooks/useAccount'

import Grid from '@mui/material/Grid'
import Message from './Message'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount,
    getAccountStatus,
    loadAccount,
    loadAccountSuccess,
    loadAccountFailure
} from '../../actions/account'

export default function Account(props) {
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const status = useSelector(getAccountStatus)
    const dispatch = useDispatch()

    const onLoadAccountCallback = useCallback(() => dispatch(loadAccount()), [dispatch])
    const onLoadAccountSuccessCallback = useCallback((account) => dispatch(loadAccountSuccess(account)), [dispatch])
    const onLoadAccountFailureCallback = useCallback((error) => dispatch(loadAccountFailure(error)), [dispatch])

    useAccount({
        appConfig: config,
        onLoadAccount: onLoadAccountCallback,
        onLoadAccountSuccess: onLoadAccountSuccessCallback,
        onLoadAccountFailure: onLoadAccountFailureCallback
    })

    return (
        account ? props.children : <div class="account-loading"><Message error={status.error} text={status.message}/></div>
    )
}

