const React = require('react')
const PropTypes = require('prop-types')

const { Row, Col } = require('react-materialize')

class Header extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        if (this.props.role == 'anonymous') {
            return <header>
                <Row>
                    <Col s={6}>
                        <span>Welcome {this.props.name}</span>
                    </Col>
                    <Col s={6} className="right-align">
                        {this.props.parent && <span><a href={this.props.parent.link}>{this.props.parent.label}</a> | </span>}
                        <span onClick={(e) => this.props.onLogin()}>Login</span>
                    </Col>
                </Row>
            </header>
        } else {
            return <header>
                <Row>
                    <Col s={6}>
                        <span>Welcome {this.props.name}</span>
                    </Col>
                    <Col s={6} className="right-align">
                        {this.props.parent && <span><a href={this.props.parent.link}>{this.props.parent.label}</a> | </span>}
                        <span onClick={(e) => this.props.onLogout()}>Logout</span>
                    </Col>
                </Row>
            </header>
        }
    }
}

Header.propTypes = {
  role: PropTypes.string,
  name: PropTypes.string,
  parent: PropTypes.object,
  onLogin: PropTypes.func,
  onLogout: PropTypes.func
}

module.exports = Header
