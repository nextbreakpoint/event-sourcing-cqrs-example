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
    getRevision,
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

        let revision = "0000000000000000-0000000000000000"

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        try {
            if (typeof(EventSource) !== "undefined") {
                axios.get(component.props.config.api_url + "/v1/watch/designs/" + this.props.uuid + "?revision=" + revision, config)
                    .then(function (response) {
                        if (response.status == 200) {
                            var source = new EventSource(response.headers.location, { withCredentials: true })

                            source.onerror = function(error) {
                               console.log(error)
                            }

                            source.onopen = function() {
                              component.loadDesign(revision)
                            }

                            source.addEventListener("update",  function(event) {
                               conosle.log(event)
                               console.log("Reloading design...")
                               component.loadDesign(revision)
                            }, false)
                        } else {
                            console.log("Can't redirect to SSE server")
                            component.loadDesign(revision)
                        }
                    })
                    .catch(function (error) {
                        console.log("Can't retrieve url of SSE server")
                        component.loadDesign(revision)
                    })
            } else {
                console.log("EventSource not available")
                component.loadDesign(revision)
            }
        } catch (e) {
           console.log("Can't subscribe: " + e)
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

        axios.get(component.props.config.api_url + '/v1/designs/' + this.props.uuid, config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Design loaded")
                    let design = JSON.parse(response.data.json)
                    design.checksum = response.data.checksum
                    design.modified = response.data.modified
                    design.uuid = response.data.uuid
                    if (component.props.design == undefined || design.script != component.props.design.script || design.metadata != component.props.design.metadata || design.manifest != component.props.design.manifest) {
                        console.log("Design changed")
                        component.props.handleLoadDesignSuccess(design, revision)
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
    revision: PropTypes.string.isRequired,
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
    revision: getRevision(state)
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

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Preview))
