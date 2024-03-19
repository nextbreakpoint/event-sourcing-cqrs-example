import React from 'react'
import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import LoadDesign from '../../commands/loadDesign'
import useDesign from '../../hooks/useDesign'

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
    getDesign,
    getDesignStatus,
    loadDesign,
    loadDesignSuccess,
    loadDesignFailure,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage
} from '../../actions/design'

export default function Design(props) {
    const abortControllerRef = useRef(new AbortController())
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const design = useSelector(getDesign)
    const status = useSelector(getDesignStatus)
    const errorMessage = useSelector(getErrorMessage)
    const isShowErrorMessage = useSelector(getShowErrorMessage)
    const dispatch = useDispatch()

    const onShowErrorMessage = useCallback((error) => dispatch(showErrorMessage(error)), [dispatch])
    const onHideErrorMessage = useCallback(() => dispatch(hideErrorMessage()), [dispatch])
    const onLoadDesign = useCallback(() => dispatch(loadDesign()), [dispatch])
    const onLoadDesignSuccess = useCallback((newDesign, newRevision) => {
        if (design == undefined || newDesign.checksum != design.checksum || newDesign.revision > design.revision) {
            console.log("Design has changed")
            dispatch(loadDesignSuccess(newDesign, newRevision))
        } else {
            console.log("Design hasn't changed")
        }
    }, [design, dispatch])
    const onLoadDesignFailure = useCallback((error) => dispatch(loadDesignFailure(error)), [dispatch])

    const doLoadDesign = useCallback((revision) => {
        const command = new LoadDesign(config, abortControllerRef)

        command.onLoadDesigns = onLoadDesign

        command.onLoadDesignSuccess = (design, revision) => {
            onLoadDesignSuccess(design, revision)
        }

        command.onLoadDesignFailure = (error) => {
            onLoadDesignFailure(error)
            onShowErrorMessage(error)
        }

        command.run(revision, props.uuid)
    }, [config, props.uuid, onShowErrorMessage, onLoadDesign, onLoadDesignSuccess, onLoadDesignFailure])

    useDesign({ uuid: props.uuid, doLoadDesign: doLoadDesign })

    return (
        <React.Fragment>
            <Grid container justify="space-between" alignItems="center">
                <Grid item xs={12}>
                    {(design && design.published == true) && <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} backText={"Show all designs"} backLink={"/admin/designs.html"} browseText={"Show fractal"} browseLink={"/browse/designs/" + props.uuid + ".html"}/>}
                    {(!design || design.published == false) && <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} backText={"Show all designs"} backLink={"/admin/designs.html"}/>}
                </Grid>
                <Grid item xs={12}>
                    {design ? (props.children) : (<div class="designs-loading"><Message error={status.error} text={status.message}/></div>)}
                </Grid>
                <Grid item xs={12}>
                    <Footer/>
                </Grid>
            </Grid>
            <ErrorPopup showErrorMessage={isShowErrorMessage} errorMessage={errorMessage} onPopupClose={onHideErrorMessage}/>
        </React.Fragment>
    )
}
