import React from 'react'
import PropTypes from 'prop-types'

import reducers from './reducers'

import Header from './Header'
import Footer from './Footer'
import Designs from './Designs'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Grid from '@material-ui/core/Grid'

import { connect } from 'react-redux'

const styles = theme => ({
  button: {
    position: 'absolute',
    bottom: theme.spacing.unit * 2,
    right: theme.spacing.unit * 2
  }
})

class DesignsPage extends React.Component {
    render() {
        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs'}/>
                    </Grid>
                    <Grid item xs={12}>
                        <Designs/>
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

    return {}
}

const mapDispatchToProps = dispatch => ({})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(DesignsPage))

