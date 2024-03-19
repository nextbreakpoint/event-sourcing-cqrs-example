import React from 'react'
import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import LoadDesigns from '../../commands/loadDesigns'
import useDesigns from '../../hooks/useDesigns'

import Grid from '@mui/material/Grid'
import Header from '../shared/Header'
import Footer from '../shared/Footer'
import Message from '../shared/Message'
import ErrorPopup from '../shared/ErrorPopup'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount
} from '../../actions/account'

import {
    getDesigns,
    getDesignsStatus,
    getRevision,
    loadDesigns,
    getPagination,
    loadDesignsSuccess,
    loadDesignsFailure,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage
} from '../../actions/designs'

export default function Designs(props) {
    const abortControllerRef = useRef(new AbortController())
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const designs = useSelector(getDesigns)
    const status = useSelector(getDesignsStatus)
    const revision = useSelector(getRevision)
    const pagination = useSelector(getPagination)
    const errorMessage = useSelector(getErrorMessage)
    const isShowErrorMessage = useSelector(getShowErrorMessage)
    const dispatch = useDispatch()

    const onShowErrorMessage = useCallback((error) => dispatch(showErrorMessage(error)), [dispatch])
    const onHideErrorMessage = useCallback(() => dispatch(hideErrorMessage()), [dispatch])
    const onLoadDesigns = useCallback(() => dispatch(loadDesigns()), [dispatch])
    const onLoadDesignsSuccess = useCallback((designs, total, revision) => dispatch(loadDesignsSuccess(designs, total, revision)), [dispatch])
    const onLoadDesignsFailure = useCallback((error) => dispatch(loadDesignsFailure(error)), [dispatch])

    const doLoadDesigns = useCallback((revision) => {
        const command = new LoadDesigns(config, abortControllerRef)

        command.onLoadDesigns = onLoadDesigns

        command.onLoadDesignsSuccess = (designs, total, revision) => {
            onLoadDesignsSuccess(designs, total, revision)
        }

        command.onLoadDesignsFailure = (error) => {
            onLoadDesignsFailure(error)
            onShowErrorMessage(error)
        }

        command.run(revision, pagination)
    }, [config, pagination, onShowErrorMessage, onLoadDesigns, onLoadDesignsSuccess, onLoadDesignsFailure])

    useDesigns({ doLoadDesigns: doLoadDesigns })

    return (
        <React.Fragment>
            <Grid container justify="space-between" alignItems="center">
                <Grid item xs={12}>
                    <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} browseText={"Browse fractals"} browseLink={"/browse/designs.html"}/>
                </Grid>
                <Grid item xs={12}>
                    {designs ? (props.children) : (<div class="design-loading"><Message error={status.error} text={status.message}/></div>)}
                </Grid>
                <Grid item xs={12}>
                    <Footer/>
                </Grid>
            </Grid>
            <ErrorPopup showErrorMessage={isShowErrorMessage} errorMessage={errorMessage} onPopupClose={onHideErrorMessage}/>
        </React.Fragment>
    )
}

