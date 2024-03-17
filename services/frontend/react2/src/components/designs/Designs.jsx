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
    const showErrorMessage = useSelector(getShowErrorMessage)
    const dispatch = useDispatch()

    const onShowErrorMessage = (error) => dispatch(showErrorMessage(error))
    const onHideErrorMessage = () => dispatch(hideErrorMessage())
    const onLoadDesigns = () => dispatch(loadDesigns())
    const onLoadDesignsSuccess = (designs, total, revision) => dispatch(loadDesignsSuccess(designs, total, revision))
    const onLoadDesignsFailure = (error) => dispatch(loadDesignsFailure(error))

    const doLoadDesigns = useCallback((revision) => {
        const command = new LoadDesigns(config, abortControllerRef)

        command.onLoadDesigns = onLoadDesigns

        command.onLoadDesignsSuccess = (designs, total, revision) => {
            onLoadDesignsSuccess(designs, total, revision)
        }

        command.onLoadDesignsFailure = (error) => {
            onLoadDesignsFailure("Can't load designs")
            onShowErrorMessage("Can't load designs")
            onLoadDesignsSuccess([], 0, 0)
        }

        command.run(revision, pagination)
    }, [config, pagination, dispatch])

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
            <ErrorPopup showErrorMessage={showErrorMessage} errorMessage={errorMessage} onPopupClose={onHideErrorMessage}/>
        </React.Fragment>
    )
}

