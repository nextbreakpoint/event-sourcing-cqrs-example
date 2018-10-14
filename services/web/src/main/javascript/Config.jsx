import React from 'react'
import ReactDOM from 'react-dom'
import PropTypes from 'prop-types'

import reducers from './reducers'

import { connect } from 'react-redux'

import { setConfig } from './actions/designs'

import axios from 'axios'

const base_url = 'https://localhost:8080'

class Config extends React.Component {
    componentDidMount = () => {
        console.log("Loading config...")

        let component = this

        let config = {
            timeout: 5000,
            withCredentials: true
        }

        axios.get(base_url + '/config', config)
            .then(function (content) {
                let config = content.data

                component.props.handleConfigLoaded(config)
            })
            .catch(function (error) {
                console.log("Can't load config " + error)
            })
    }

    render() {
        let children = this.props.children

        if (this.props.config) {
            return (
                <div>{children}</div>
            )
        } else {
            return (
                <div><p>Loading configuration...</p></div>
            )
        }
    }
}

const mapStateToProps = state => {
    //console.log(JSON.stringify(state))

    return {
        config: state.designs.config
    }
}

const mapDispatchToProps = dispatch => ({
    handleConfigLoaded: (config) => {
        dispatch(setConfig(config))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(Config)

