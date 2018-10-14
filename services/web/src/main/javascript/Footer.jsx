import React from 'react'
import PropTypes from 'prop-types'

class Footer extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        return <footer><span>Powered by NextBreakpoint</span></footer>
    }
}

Footer.propTypes = {
  role: PropTypes.string,
  name: PropTypes.string
}

export default Footer
