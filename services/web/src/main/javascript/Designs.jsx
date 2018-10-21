import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

import Message from './Message'

import { connect } from 'react-redux'

import {
    getConfig,
    getAccount,
    getDesigns,
    getDesignsStatus,
    getTimestamp,
    loadDesigns,
    loadDesignsSuccess,
    loadDesignsFailure
} from './actions/designs'

import axios from 'axios'

let Designs = class Designs extends React.Component {
    componentDidMount = () => {
        let timestamp = Date.now();

        let component = this

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.props.config.web_url + "/watch/designs/" + timestamp, { withCredentials: true })

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
                console.log("EventSource not available")
            }
        } catch (e) {
           console.log(e)
        }
    }

    loadDesigns = (timestamp) => {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        component.props.handleLoadDesigns()

        axios.get(component.props.config.designs_query_url, config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    let designs = response.data.map((design) => { return { uuid: design.uuid, checksum: design.checksum, selected: false }})
                    component.props.handleLoadDesignsSuccess(designs, timestamp)
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    component.props.handleLoadDesignsSuccess([], timestamp)
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                component.props.handleLoadDesignsSuccess([], timestamp)
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
