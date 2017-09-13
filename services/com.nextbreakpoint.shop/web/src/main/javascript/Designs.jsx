const React = require('react')
const PropTypes = require('prop-types')

const { Card, Button, Table, Input } = require('react-materialize')

const DesignItem = require('./DesignItem')

let Designs = class Designs extends React.Component {
    constructor(props) {
        super(props)

        this.renderList = this.renderList.bind(this)
        this.renderItem = this.renderItem.bind(this)
    }

    renderItem(config, role, modified, design) {
        return <DesignItem config={config} role={role} key={design.uuid} design={design} modified={modified} onSelect={this.props.onSelect}/>
    }

    renderList() {
        return this.props.designs.map(design => this.renderItem(this.props.config, this.props.role, this.props.modified, design))
    }

    render() {
        if (this.props.role == 'admin') {
            return <Card title="List of designs" className="hoverable">
                <div className="card-content">
                    <Table>
                        <thead>
                            <tr><th></th><th>Preview</th><th>UUID</th></tr>
                        </thead>
                        <tbody>
                            {this.renderList()}
                        </tbody>
                    </Table>
                </div>
                <div class="card-action">
                    <Button waves='light' onClick={this.props.onDelete}>Delete</Button>
                </div>
            </Card>
        } else {
            return <Card title="List of designs" className="hoverable">
                <div className="card-content">
                    <Table>
                        <thead>
                            <tr><th></th><th>Preview</th><th>UUID</th></tr>
                        </thead>
                        <tbody>
                            {this.renderList()}
                        </tbody>
                    </Table>
                </div>
            </Card>
        }
    }
}

Designs.propTypes = {
  config: PropTypes.object,
  designs: PropTypes.object,
  modified: PropTypes.string,
  role: PropTypes.string,
  onDelete: PropTypes.func,
  onSelect: PropTypes.func
}

module.exports = Designs
