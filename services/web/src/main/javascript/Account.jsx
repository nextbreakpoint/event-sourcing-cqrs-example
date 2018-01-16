const React = require('react')
const PropTypes = require('prop-types')

class Account extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        return <p>User role is {this.props.role}</p>
    }
}

Account.propTypes = {
  role: PropTypes.string
}

module.exports = Account
