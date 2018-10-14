import React from 'react'
import PropTypes from 'prop-types'

import Grid from '@material-ui/core/Grid'

class Header extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        return (
            <header>
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={6}>
                        <span>Welcome {this.props.name}</span>
                    </Grid>
                    <Grid item xs={6} className="right-align">
                        {this.props.parent && <span><a href={this.props.parent.link}>{this.props.parent.label}</a> | </span>}
                        {this.props.role == 'anonymous' && <span onClick={(e) => this.props.onLogin()}>Login</span>}
                        {this.props.role != 'anonymous' && <span onClick={(e) => this.props.onLogout()}>Logout</span>}
                    </Grid>
                </Grid>
            </header>
        )
    }
}

Header.propTypes = {
  role: PropTypes.string,
  name: PropTypes.string,
  parent: PropTypes.object,
  onLogin: PropTypes.func,
  onLogout: PropTypes.func
}

export default Header
