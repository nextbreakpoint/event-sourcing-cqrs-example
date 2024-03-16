import React from 'react'
import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import DesignCommand from '../../commands/design'
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
    getRevision,
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
    const revision = useSelector(getRevision)
    const errorMessage = useSelector(getErrorMessage)
    const showErrorMessage = useSelector(getShowErrorMessage)
    const dispatch = useDispatch()

    const onShowErrorMessage = (error) => dispatch(showErrorMessage(error))
    const onHideErrorMessage = () => dispatch(hideErrorMessage())
    const onLoadDesign = () => dispatch(loadDesign())
    const onLoadDesignSuccess = (newDesign, newRevision) => {
        console.log("Design changed?")
        if (design == undefined || newDesign.checksum != design.checksum || newDesign.revision > design.revision) {
            console.log("Design changed")
            dispatch(loadDesignSuccess(newDesign, newRevision))
        }
    }
    const onLoadDesignFailure = (error) => dispatch(loadDesignFailure(error))

    const doLoadDesign = useCallback((revision) => {
        const designCommand = new DesignCommand(config, abortControllerRef)

        designCommand.onLoadDesigns = onLoadDesign

        designCommand.onLoadDesignSuccess = (design, revision) => {
            onLoadDesignSuccess(design, revision)
        }

        designCommand.onLoadDesignFailure = (error) => {
            onLoadDesignFailure("Can't load design")
            onShowErrorMessage("Can't load design")
        }

        designCommand.load(revision, props.uuid)
    }, [config, dispatch, props.uuid])

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
            <ErrorPopup showErrorMessage={showErrorMessage} errorMessage={errorMessage} onPopupClose={onHideErrorMessage}/>
        </React.Fragment>
    )
}
