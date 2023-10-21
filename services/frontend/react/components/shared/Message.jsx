import React from 'react'
import PropTypes from 'prop-types'

import Grid from '@mui/material/Grid'
import Snackbar from '@mui/material/Snackbar'
import IconButton from '@mui/material/IconButton'

import CloseIcon from '@mui/icons-material/Close'

import { connect } from 'react-redux'

import {
    getShowErrorMessage,
    getErrorMessage
} from '../../actions/designs'

let Message = class Message extends React.Component {
    render() {
        return (
            <React.Fragment>
                <Grid container justify="center" className={this.props.error ? 'message-error' : 'message-normal'}>
                    <Grid item xs={12}>{this.props.text}</Grid>
                </Grid>
                <Snackbar
                  anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                  }}
                  open={this.props.show_error_message}
                  autoHideDuration={6000}
                  onClose={this.handleClose}
                  ContentProps={{
                    'aria-describedby': 'message-id',
                  }}
                  message={<span id="message-id">{this.props.error_message}</span>}
                  action={[
                    <IconButton
                      key="close"
                      aria-label="Close"
                      color="inherit"
                      onClick={this.handleClose}
                    >
                      <CloseIcon />
                    </IconButton>
                  ]}
                />
            </React.Fragment>
        )
    }
}

Message.propTypes = {
    text: PropTypes.string.isRequired,
    error: PropTypes.bool.isRequired,
    show_error_message: PropTypes.bool.isRequired,
    error_message: PropTypes.string.isRequired
}

const mapStateToProps = state => ({
    show_error_message: getShowErrorMessage(state),
    error_message: getErrorMessage(state)
})

const mapDispatchToProps = dispatch => ({})

export default connect(mapStateToProps, mapDispatchToProps)(Message)
