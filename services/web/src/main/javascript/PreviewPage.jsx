import React from 'react'
import PropTypes from 'prop-types'

import Header from './Header'
import Footer from './Footer'
import Preview from './Preview'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Grid from '@material-ui/core/Grid'

import { connect } from 'react-redux'

var uuid = "00000000-0000-0000-0000-000000000000"

const regexp = /https?:\/\/.*\/admin\/designs\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/g
const match = regexp.exec(window.location.href)

if (match != null && match.length == 2) {
    uuid = match[1]
}

const base_url = 'https://localhost:8080'

const styles = theme => ({
  button: {
    position: 'absolute',
    bottom: theme.spacing.unit * 2,
    right: theme.spacing.unit * 2
  }
})

class PreviewPage extends React.Component {
    render() {
        const url = this.props.config.designs_query_url + '/' + uuid + '/{z}/{x}/{y}/256.png?t=0'

        const parent = { label: 'Designs', link: base_url + '/admin/designs' }

        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs/' + uuid} parent={parent}/>
                    </Grid>
                    <Grid item xs={12}>
                        <Preview/>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer/>
                    </Grid>
                </Grid>
            </React.Fragment>
        )
    }
}

const mapStateToProps = state => {
    //console.log(JSON.stringify(state))

    return {
        config: state.designs.config
    }
}

const mapDispatchToProps = dispatch => ({})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(PreviewPage))
