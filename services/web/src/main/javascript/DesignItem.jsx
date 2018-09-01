const React = require('react')
const PropTypes = require('prop-types')

const { Input } = require('react-materialize')

let DesignItem = class DesignItem extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        let design = this.props.design

        if (this.props.role == 'admin') {
            if (design.selected) {
                return <tr><td><Input label=" " defaultChecked="checked" className="filled-in" name="uuid" id={"uuid-" + design.uuid} type="checkbox" onClick={(e) => this.props.onSelect(design.uuid, false)}/></td><td><img className="z-depth-3" width={128} height={128} src={this.props.config.designs_url + "/api/designs/" + design.uuid + "/0/0/0/256.png?t=" + this.props.timestamp}/></td><td><a href={"/admin/designs/" + design.uuid}>{design.uuid}</a></td></tr>
            } else {
                return <tr><td><Input label=" "                          className="filled-in" name="uuid" id={"uuid-" + design.uuid} type="checkbox" onClick={(e) => this.props.onSelect(design.uuid, true )}/></td><td><img className="z-depth-3" width={128} height={128} src={this.props.config.designs_url + "/api/designs/" + design.uuid + "/0/0/0/256.png?t=" + this.props.timestamp}/></td><td><a href={"/admin/designs/" + design.uuid}>{design.uuid}</a></td></tr>
            }
        } else {
            return <tr><td></td><td><img className="z-depth-3" width={128} height={128} src={this.props.config.designs_url + "/api/designs/" + design.uuid + "/0/0/0/256.png?t=" + this.props.timestamp}/></td><td><a href={"/admin/designs/" + design.uuid}>{design.uuid}</a></td></tr>
        }
    }
}

DesignItem.propTypes = {
  config: PropTypes.object,
  design: PropTypes.object,
  timestamp: PropTypes.string,
  role: PropTypes.string,
  onSelect: PropTypes.func
}

module.exports = DesignItem
