import React from 'react'
import PropTypes from 'prop-types'

import Header from '../shared/Header'
import Footer from '../shared/Footer'
import Message from '../shared/Message'

import Grid from '@mui/material/Grid'
import Snackbar from '@mui/material/Snackbar'
import IconButton from '@mui/material/IconButton'
import Input from '@mui/material/Input'

import CloseIcon from '@mui/icons-material/Close'

import { connect } from 'react-redux'

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
} from '../../actions/preview'

import axios from 'axios'

let Preview = class Preview extends React.Component {
    componentDidMount = () => {
        let component = this

        let revision = "0000000000000000-0000000000000000"

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.props.config.api_url + "/v1/designs/watch?designId=" + this.props.uuid + "&revision=" + revision, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                  component.loadDesign(revision)
                }

                source.addEventListener("update",  function(event) {
                   console.log(event.data)
                   console.log("Reloading design...")
                   component.loadDesign(revision)
                }, false)
            } else {
                console.log("Can't watch resource. EventSource not supported by the browser")
                component.loadDesign(revision)
            }
        } catch (e) {
           console.log("Can't watch resource: " + e)
           component.loadDesign(revision)
        }
    }

    loadDesign = (revision) => {
        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        component.props.handleLoadDesign()

        function computePercentage(design, levels) {
            let total = levels.map(i => design.tiles[i].total)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

            let completed = levels.map(i => design.tiles[i].completed)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

            let percentage = Math.floor((completed * 100.0) / total)

            return percentage
        }

        axios.get(component.props.config.api_url + '/v1/designs/' + this.props.uuid + '?draft=true', config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Design loaded")
                    let design = response.data
                    let data = JSON.parse(design.json)
                    design.manifest = data.manifest
                    design.metadata = data.metadata
                    design.script = data.script
                    design.draft = design.levels != 8
                    design.published = design.published
                    design.percentage = computePercentage(design, [0,1,2,3,4,5,6,7])
                    design.preview_percentage = computePercentage(design, [0,1,2])
                    console.log(design)
                    if (component.props.design == undefined || design.revision > component.props.revision || design.checksum != component.props.checksum) {
                        console.log("Design changed")
                        component.props.handleLoadDesignSuccess(design, design.revision)
                    }
                } else {
                    console.log("Can't load design: status = " + content.status)
                    component.props.handleLoadDesignFailure("Design not found")
                    component.props.handleShowErrorMessage("Can't load design")
                }
            })
            .catch(function (error) {
                console.log("Can't load design: " + error)
                component.props.handleLoadDesignFailure("Design not found")
                component.props.handleShowErrorMessage("Can't load design")
            })
    }

    handleClose = (event, reason) => {
        if (reason === 'clickaway') {
          return
        }

        this.props.handleHideErrorMessage()
    }

    render() {
        return (
            <React.Fragment>
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        {(this.props.design && this.props.design.published == true) && <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} backText={"Show all designs"} backLink={"/admin/designs.html"} browseText={"Show fractal"} browseLink={"/browse/designs/" + this.props.uuid + ".html"}/>}
                        {(!this.props.design || this.props.design.published == false) && <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} backText={"Show all designs"} backLink={"/admin/designs.html"}/>}
                    </Grid>
                    <Grid item xs={12}>
                        {this.props.design ? (this.props.children) : (<div class="designs-loading"><Message error={this.props.status.error} text={this.props.status.message}/></div>)}
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
                  open={this.props.show_error_message}
                  autoHideDuration={6000}
                  onClose={this.handleClose}
                  ContentProps={{
                    'aria-describedby': 'message-id',
                  }}
                  message={<span id="message-id">{this.props.error_message}</span>}
                  action={[
                    <IconButton
                      key="close"
                      aria-label="Close"
                      color="inherit"
                      onClick={this.handleClose}
                    >
                      <CloseIcon />
                    </IconButton>
                  ]}
                />
            </React.Fragment>
        )
    }
}

Preview.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    status: PropTypes.object.isRequired,
    revision: PropTypes.string.isRequired,
    uuid: PropTypes.string.isRequired,
    show_error_message: PropTypes.bool.isRequired,
    error_message: PropTypes.string.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    design: getDesign(state),
    status: getDesignStatus(state),
    revision: getRevision(state),
    show_error_message: getShowErrorMessage(state),
    error_message: getErrorMessage(state)
})

const mapDispatchToProps = dispatch => ({
    handleShowErrorMessage: (error) => {
        dispatch(showErrorMessage(error))
    },
    handleHideErrorMessage: () => {
        dispatch(hideErrorMessage())
    },
    handleLoadDesign: () => {
        dispatch(loadDesign())
    },
    handleLoadDesignSuccess: (design, revision) => {
        dispatch(loadDesignSuccess(design, revision))
    },
    handleLoadDesignFailure: (error) => {
        dispatch(loadDesignFailure(error))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Preview)
