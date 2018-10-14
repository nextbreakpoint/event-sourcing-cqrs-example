import React from 'react'
import PropTypes from 'prop-types'

import Checkbox from '@material-ui/core/Checkbox'

let DesignItem = class DesignItem extends React.Component {
    constructor(props) {
        super(props)
        this.renderImage = this.renderImage.bind(this)
        this.renderAnchor = this.renderAnchor.bind(this)
        this.renderCheckbox = this.renderCheckbox.bind(this)
    }

    renderImage(design) {
        return <img width={128} height={128} src={this.props.config.designs_query_url + "/" + design.uuid + "/0/0/0/256.png?t=" + this.props.timestamp}/>
    }

    renderAnchor(design) {
        return <a href={"/admin/designs/" + design.uuid}>{design.uuid}</a>
    }

    renderCheckbox(design) {
        return this.props.role == 'admin' && <Checkbox checked={design.selected} value="uuid" id={"uuid-" + design.uuid} onChange={(e) => this.props.onSelect(design.uuid, e.currentTarget.checked)}/>
    }

    render() {
        let design = this.props.design

        let image = this.renderImage(design)
        let anchor = this.renderAnchor(design)
        let checkbox = this.renderCheckbox(design)

        return <tr><td>{checkbox}</td><td>{image}</td><td>{anchor}</td></tr>
    }
}

DesignItem.propTypes = {
  config: PropTypes.object,
  design: PropTypes.object,
  timestamp: PropTypes.string,
  role: PropTypes.string,
  onSelect: PropTypes.func
}

export default DesignItem
