import React from 'react'
import { useSelector } from 'react-redux'
import useConfig from '../../hooks/useConfig'

import Grid from '@mui/material/Grid'
import Message from './Message'

import {
    getConfig,
    getConfigStatus
} from '../../actions/config'

export default function Config(props) {
    const config = useSelector(getConfig)
    const status = useSelector(getConfigStatus)

    useConfig()

    return (
        config ? props.children : <div><Message error={status.error} text={status.message}/></div>
    )
}
