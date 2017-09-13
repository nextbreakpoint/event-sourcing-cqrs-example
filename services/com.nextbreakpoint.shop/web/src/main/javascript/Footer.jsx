const React = require('react')
const PropTypes = require('prop-types')

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

module.exports = Footer
