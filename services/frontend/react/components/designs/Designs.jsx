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
    getRevision,
    loadDesigns,
    getPage,
    getRowsPerPage,
    loadDesignsSuccess,
    loadDesignsFailure,
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
        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        component.props.handleLoadDesigns()

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

        axios.get(component.props.config.api_url + '/v1/designs?draft=true&from=' + (component.props.page * component.props.rowsPerPage) + '&size=' + component.props.rowsPerPage, config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    let designs = response.data.designs.map((design) => { return { uuid: design.uuid, checksum: design.checksum, revision: design.revision, levels: design.levels, created: design.created, updated: design.updated, draft: design.levels != 8, published: design.published, percentage: computePercentage(design) }})
                    let total = response.data.total
                    component.props.handleLoadDesignsSuccess(designs, total, revision)
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    component.props.handleLoadDesignsSuccess([], 0, 0)
                    component.props.handleShowErrorMessage("Can't load designs")
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                component.props.handleLoadDesignsSuccess([], 0, 0)
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
    revision: PropTypes.string.isRequired,
    page: PropTypes.number,
    rowsPerPage: PropTypes.number,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired
}

const styles = theme => ({})

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    status: getDesignsStatus(state),
    revision: getRevision(state),
    page: getPage(state),
    rowsPerPage: getRowsPerPage(state)
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
    handleLoadDesignsSuccess: (designs, revision) => {
        dispatch(loadDesignsSuccess(designs, revision))
    },
    handleLoadDesignsFailure: (error) => {
        dispatch(loadDesignsFailure(error))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Designs))
