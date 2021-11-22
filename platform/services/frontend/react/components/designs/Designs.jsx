import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

import Message from '../shared/Message'

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
    getTimestamp,
    loadDesigns,
    loadDesignsSuccess,
    loadDesignsFailure,
    showErrorMessage,
    hideErrorMessage
} from '../../actions/designs'

import axios from 'axios'

let Designs = class Designs extends React.Component {
    componentDidMount = () => {
        let timestamp = Date.now();

        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        try {
            if (typeof(EventSource) !== "undefined") {
                axios.get(component.props.config.api_url + "/v1/watch/designs/" + timestamp, config)
                    .then(function (response) {
                        if (response.status == 200) {
                            var source = new EventSource(response.headers.location, { withCredentials: true })

                            source.onerror = function(error) {
                               console.log(error)
                            }

                            source.onopen = function() {
                              component.loadDesigns(timestamp)
                            }

                            source.addEventListener("update",  function(event) {
                               let timestamp = Number(event.lastEventId)

                               if (component.props.timestamp == undefined || timestamp > component.props.timestamp) {
                                  console.log("Reloading designs...")
                                  component.loadDesigns(timestamp)
                               }
                            }, false)
                        } else {
                            console.log("Can't redirect to SSE server")
                            component.loadDesigns(timestamp)
                        }
                    })
                    .catch(function (error) {
                        console.log("Can't retrieve url of SSE server")
                        component.loadDesigns(timestamp)
                    })
            } else {
                console.log("EventSource not available")
                component.loadDesigns(timestamp)
            }
        } catch (e) {
           console.log("Can't subscribe: " + e)
           component.loadDesigns(timestamp)
        }
    }

    loadDesigns = (timestamp) => {
        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        component.props.handleLoadDesigns()

        axios.get(component.props.config.api_url + '/v1/designs', config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    let designs = response.data.map((design) => { return { uuid: design.uuid, checksum: design.checksum }})
                    component.props.handleLoadDesignsSuccess(designs, timestamp)
                    //component.props.handleHideErrorMessage()
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    component.props.handleLoadDesignsSuccess([], 0)
                    component.props.handleShowErrorMessage("Can't load designs")
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                component.props.handleLoadDesignsSuccess([], 0)
                component.props.handleShowErrorMessage("Can't load designs")
            })
    }

    render() {
        return (
            this.props.designs ? (this.props.children) : (<Message error={this.props.status.error} text={this.props.status.message}/>)
        )
    }
}

Designs.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    status: PropTypes.object.isRequired,
    timestamp: PropTypes.number.isRequired,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired
}

const styles = theme => ({})

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    status: getDesignsStatus(state),
    timestamp: getTimestamp(state)
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
    handleLoadDesignsSuccess: (designs, timestamp) => {
        dispatch(loadDesignsSuccess(designs, timestamp))
    },
    handleLoadDesignsFailure: (error) => {
        dispatch(loadDesignsFailure(error))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Designs))
