import React from 'react'
import PropTypes from 'prop-types'

import Grid from '@material-ui/core/Grid'

let Message = class Message extends React.Component {
    render() {
        return (
            <Grid container justify="center" className={this.props.error ? 'message-error' : 'message-normal'}>
                <Grid item xs={12}>{this.props.text}</Grid>
            </Grid>
        )
    }
}

Message.propTypes = {
  text: PropTypes.string.isRequired,
  error: PropTypes.bool.isRequired
}

export default Message
