import React from 'react'
import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import DesignsCommand from '../../commands/designs'

import Grid from '@mui/material/Grid'
import Snackbar from '@mui/material/Snackbar'
import IconButton from '@mui/material/IconButton'
import Input from '@mui/material/Input'
import Header from '../shared/Header'
import Footer from '../shared/Footer'
import Message from '../shared/Message'

import CloseIcon from '@mui/icons-material/Close'

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

import axios from 'axios'

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
        const designs = new DesignsCommand(config, abortControllerRef)

        designs.onLoadDesigns = onLoadDesigns

        designs.onLoadDesignsSuccess = (designs, total, revision) => {
            onLoadDesignsSuccess(designs, total, revision)
        }

        designs.onLoadDesignsFailure = (error) => {
            onLoadDesignsFailure("Can't load designs")
            onShowErrorMessage("Can't load designs")
        }

        designs.load(revision, pagination)
    }, [config, pagination, dispatch])

    useEffect(() => {
        try {
            if (typeof(EventSource) !== "undefined") {
                const source = new EventSource(config.api_url + "/v1/designs/watch?revision=" + revision, { withCredentials: true })

                const revision = "0000000000000000-0000000000000000"

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                    doLoadDesigns(revision)
                }

                source.addEventListener("update",  function(event) {
                    console.log(event.data)
                    doLoadDesigns(revision)
                }, false)

                return () => {
                    source.close()
                }
            } else {
                console.log("Can't watch resource. EventSource not supported by the browser")
                doLoadDesigns(revision)
            }
        } catch (e) {
           console.log("Can't watch resource: " + e)
           doLoadDesigns(revision)
        }

        return () => {}
    }, [config, doLoadDesigns])

    const onClose = (event, reason) => {
        if (reason === 'clickaway') {
          return
        }

        onHideErrorMessage()
    }

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
            <Snackbar
              anchorOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              open={showErrorMessage}
              autoHideDuration={6000}
              onClose={onClose}
              message={errorMessage}
              action={[
                <IconButton
                  key="close"
                  aria-label="Close"
                  color="inherit"
                  onClick={onClose}
                >
                  <CloseIcon />
                </IconButton>
              ]}
            />
        </React.Fragment>
    )
}

