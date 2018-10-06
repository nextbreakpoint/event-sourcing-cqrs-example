import React from 'react'
import PropTypes from 'prop-types'

import Button from '@material-ui/core/Button'
import Card from '@material-ui/core/Card'
import CardHeader from '@material-ui/core/CardHeader'
import CardContent from '@material-ui/core/CardContent'
import CardActions from '@material-ui/core/CardActions'
import Table from '@material-ui/core/Table'
import TableHead from '@material-ui/core/TableHead'
import TableBody from '@material-ui/core/TableBody'
import TableRow from '@material-ui/core/TableRow'
import TableCell from '@material-ui/core/TableCell'

import DesignItem from './DesignItem'

let Designs = class Designs extends React.Component {
    constructor(props) {
        super(props)

        this.renderList = this.renderList.bind(this)
        this.renderItem = this.renderItem.bind(this)
    }

    renderItem(config, role, timestamp, design) {
        return <DesignItem config={config} role={role} key={design.uuid} design={design} timestamp={timestamp} onSelect={this.props.onSelect}/>
    }

    renderList() {
        return this.props.designs.map(design => this.renderItem(this.props.config, this.props.role, this.props.timestamp, design))
    }

    render() {
        return (
            <Card>
                <CardHeader title="List of designs"></CardHeader>
                <CardContent>
                    <Table>
                        <TableHead>
                            <TableRow><TableCell></TableCell><TableCell>Preview</TableCell><TableCell>UUID</TableCell></TableRow>
                        </TableHead>
                        <TableBody>
                            {this.renderList()}
                        </TableBody>
                    </Table>
                </CardContent>
                <CardActions>
                    {this.props.role == 'admin' && <Button onClick={this.props.onDelete}>Delete</Button>}
                </CardActions>
            </Card>
        )
    }
}

Designs.propTypes = {
  config: PropTypes.object,
  designs: PropTypes.object,
  timestamp: PropTypes.string,
  role: PropTypes.string,
  onDelete: PropTypes.func,
  onSelect: PropTypes.func
}

module.exports = Designs
