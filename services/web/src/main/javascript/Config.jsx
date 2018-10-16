import React from 'react'
import ReactDOM from 'react-dom'
import PropTypes from 'prop-types'

import Grid from '@material-ui/core/Grid'

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
        return (
            <Grid container justify="space-between" alignItems="center">
                <Grid item xs={12}>
                    {this.props.config ? (this.props.children) : (<p>Loading configuration...</p>)}
                </Grid>
            </Grid>
        )
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

