import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

const styles = theme => ({
})

class Footer extends React.Component {
    render() {
        return <footer><span>Powered by NextBreakpoint</span></footer>
    }
}

Footer.propTypes = {}

export default withStyles(styles, { withTheme: true })(Footer)