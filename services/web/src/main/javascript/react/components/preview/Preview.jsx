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
    getDesign,
    getDesignStatus,
    getTimestamp,
    loadDesign,
    loadDesignSuccess,
    loadDesignFailure,
    showErrorMessage,
    hideErrorMessage
} from '../../actions/preview'

import axios from 'axios'

let Preview = class Preview extends React.Component {
    componentDidMount = () => {
        let component = this

        let timestamp = Date.now()

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.props.config.designs_sse_url + "/" + timestamp + "/" + this.props.uuid, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                  component.loadDesign(timestamp)
                }

                source.addEventListener("update",  function(event) {
                   let timestamp = Number(event.lastEventId)

                   if (component.props.timestamp == undefined || timestamp > component.props.timestamp) {
                      console.log("Reloading design...")
                      component.loadDesign(timestamp)
                   }
                }, false)
            } else {
                console.log("EventSource not available")
                component.loadDesign(timestamp)
            }
        } catch (e) {
           console.log("Can't subscribe: " + e)
           component.loadDesign(timestamp)
        }
    }

    loadDesign = (timestamp) => {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        component.props.handleLoadDesign()

        axios.get(component.props.config.designs_query_url + '/' + this.props.uuid, config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Design loaded")
                    let design = JSON.parse(response.data.json)
                    if (component.props.design == undefined || design.script != component.props.design.script || design.metadata != component.props.design.metadata || design.manifest != component.props.design.manifest) {
                        console.log("Design changed")
                        component.props.handleLoadDesignSuccess(design, timestamp)
                    }
                    //component.props.handleHideErrorMessage()
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

    render() {
        return (
            this.props.design ? (this.props.children) : (<Message error={this.props.status.error} text={this.props.status.message}/>)
        )
    }
}

Preview.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    status: PropTypes.object.isRequired,
    timestamp: PropTypes.number.isRequired,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired,
    uuid: PropTypes.string.isRequired
}

const styles = theme => ({})

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    design: getDesign(state),
    status: getDesignStatus(state),
    timestamp: getTimestamp(state)
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
    handleLoadDesignSuccess: (design, timestamp) => {
        dispatch(loadDesignSuccess(design, timestamp))
    },
    handleLoadDesignFailure: (error) => {
        dispatch(loadDesignFailure(error))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Preview))
