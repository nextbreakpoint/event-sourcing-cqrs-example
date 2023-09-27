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

        function computePercentage(design) {
            const levels = [0,1,2,3,4,5,6,7];
            let total = levels.map(i => design.tiles[i].total)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)
            let completed = levels.map(i => design.tiles[i].completed)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)
            let percentage = Math.floor((completed * 100.0) / total)
            console.log("uuid = " + design.uuid + ", percentage = " + percentage)
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
                    design.percentage = computePercentage(design)
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
