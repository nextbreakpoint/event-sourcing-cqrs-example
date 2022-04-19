import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

let Footer = class Footer extends React.Component {
    render() {
        return <footer><span>Powered by <a href="https://nextbreakpoint.com">NextBreakpoint</a></span></footer>
    }
}

Footer.propTypes = {
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired
}

const styles = theme => ({})

export default withStyles(styles, { withTheme: true })(Footer)