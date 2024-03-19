import React from 'react'
import { useSelector } from 'react-redux'
import useAccount from '../../hooks/useAccount'

import Grid from '@mui/material/Grid'
import Message from './Message'

import {
    getAccount,
    getAccountStatus
} from '../../actions/account'

export default function Account(props) {
    const account = useSelector(getAccount)
    const status = useSelector(getAccountStatus)

    useAccount()

    return (
        account ? props.children : <div class="account-loading"><Message error={status.error} text={status.message}/></div>
    )
}

