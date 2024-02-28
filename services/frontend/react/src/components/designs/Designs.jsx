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

let Designs = class Designs extends React.Component {
    componentDidMount = () => {
        let component = this

        let revision = "0000000000000000-0000000000000000"

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.props.config.api_url + "/v1/designs/watch?revision=" + revision, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                  component.loadDesigns(revision)
                }

                source.addEventListener("update",  function(event) {
                   console.log(event.data)
                   console.log("Reloading designs...")
                   component.loadDesigns(revision)
                }, false)
            } else {
                console.log("Can't watch resource. EventSource not supported by the browser")
                component.loadDesigns(revision)
            }
        } catch (e) {
           console.log("Can't watch resource: " + e)
           component.loadDesigns(revision)
        }
    }

    loadDesigns = (revision) => {
        console.log("Load designs")

        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        component.props.handleLoadDesigns()

        function computePercentage(design, levels) {
            let total = levels.map(i => design.tiles[i].total)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

            let completed = levels.map(i => design.tiles[i].completed)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

            let percentage = Math.floor((completed * 100.0) / total)

            return percentage
        }

        axios.get(component.props.config.api_url + '/v1/designs?draft=true&from=' + (component.props.pagination.page * component.props.pagination.pageSize) + '&size=' + component.props.pagination.pageSize, config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    let designs = response.data.designs.map((design) => { return { uuid: design.uuid, checksum: design.checksum, revision: design.revision, levels: design.levels, created: design.created, updated: design.updated, draft: design.levels != 8, published: design.published, percentage: computePercentage(design, [0,1,2,3,4,5,6,7]), preview_percentage: computePercentage(design, [0,1,2]), json: design.json }})
                    let total = response.data.total
                    component.props.handleLoadDesignsSuccess(designs, total, revision)
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    component.props.handleLoadDesignsFailure("Can't load designs")
                    component.props.handleShowErrorMessage("Can't load designs")
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                component.props.handleLoadDesignsFailure("Can't load designs")
                component.props.handleShowErrorMessage("Can't load designs")
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
                        <Header landing={'/admin/designs.html'} titleText={"Fractals"} subtitleText={"The Beauty of Chaos"} browseText={"Browse fractals"} browseLink={"/browse/designs.html"}/>
                    </Grid>
                    <Grid item xs={12}>
                        {this.props.designs ? (this.props.children) : (<div class="design-loading"><Message error={this.props.status.error} text={this.props.status.message}/></div>)}
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

Designs.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    status: PropTypes.object.isRequired,
    revision: PropTypes.string.isRequired,
    pagination: PropTypes.object,
    show_error_message: PropTypes.bool.isRequired,
    error_message: PropTypes.string.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    status: getDesignsStatus(state),
    revision: getRevision(state),
    pagination: getPagination(state),
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
    handleLoadDesigns: () => {
        dispatch(loadDesigns())
    },
    handleLoadDesignsSuccess: (designs, total, revision) => {
        dispatch(loadDesignsSuccess(designs, total, revision))
    },
    handleLoadDesignsFailure: (designs, total, revision) => {
        dispatch(loadDesignsFailure(designs, total, revision))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Designs)
