import React from 'react'
import PropTypes from 'prop-types'

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
