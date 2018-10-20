import React from 'react'
import PropTypes from 'prop-types'
import classNames from 'classnames'

import { withStyles } from '@material-ui/core/styles'
import { lighten } from '@material-ui/core/styles/colorManipulator'

import Table from '@material-ui/core/Table'
import TableBody from '@material-ui/core/TableBody'
import TableCell from '@material-ui/core/TableCell'
import TableHead from '@material-ui/core/TableHead'
import TablePagination from '@material-ui/core/TablePagination'
import TableRow from '@material-ui/core/TableRow'
import TableSortLabel from '@material-ui/core/TableSortLabel'
import Toolbar from '@material-ui/core/Toolbar'
import Typography from '@material-ui/core/Typography'
import Paper from '@material-ui/core/Paper'
import Checkbox from '@material-ui/core/Checkbox'
import IconButton from '@material-ui/core/IconButton'
import ButtonBase from '@material-ui/core/ButtonBase'
import Tooltip from '@material-ui/core/Tooltip'

import AddIcon from '@material-ui/icons/Add'
import EditIcon from '@material-ui/icons/Edit'
import DeleteIcon from '@material-ui/icons/Delete'

import { connect } from 'react-redux'

import { changePage, changeRowsPerPage, showCreateDesign, showDeleteDesigns, setPage, setRowsPerPage, setOrder, setSelected } from './actions/designs'

import axios from 'axios'

const base_url = 'https://localhost:8080'

function createData(uuid) {
  return { uuid: uuid }
}

function desc(a, b, orderBy) {
  if (b[orderBy] < a[orderBy]) {
    return -1
  }
  if (b[orderBy] > a[orderBy]) {
    return 1
  }
  return 0
}

function stableSort(array, cmp) {
  const stabilizedThis = array.map((el, index) => [el, index])
  stabilizedThis.sort((a, b) => {
    const order = cmp(a[0], b[0])
    if (order !== 0) return order
    return a[1] - b[1]
  })
  return stabilizedThis.map(el => el[0])
}

function getSorting(order, orderBy) {
  return order === 'desc' ? (a, b) => desc(a, b, orderBy) : (a, b) => -desc(a, b, orderBy)
}

const cells = [
  { id: 'uuid', numeric: false, disablePadding: true, label: 'UUID', enableSort: true, className: '' },
  { id: 'image', numeric: false, disablePadding: true, label: '', enableSort: false, className: 'list-image' }
]

class EnhancedTableHead extends React.Component {
  createSortHandler = property => event => {
    this.props.onRequestSort(event, property)
  }

  render() {
    const { onSelectAllClick, order, orderBy, numSelected, rowCount, role } = this.props

    return (
      <TableHead>
        <TableRow>
          <TableCell padding="checkbox">
            {role == 'admin' && <Checkbox
              indeterminate={numSelected > 0 && numSelected < rowCount}
              checked={numSelected === rowCount}
              onChange={onSelectAllClick}
            />}
          </TableCell>
          {cells.map(cell => {
            return (
              cell.enableSort == true ? (
              <TableCell
                key={cell.id}
                numeric={cell.numeric}
                padding={cell.disablePadding ? 'none' : 'default'}
                sortDirection={orderBy === cell.id ? order : false}
                className={cell.className}
              >
                <Tooltip
                  title="Sort"
                  placement={cell.numeric ? 'bottom-end' : 'bottom-start'}
                  enterDelay={300}
                >
                  <TableSortLabel
                    active={orderBy === cell.id}
                    direction={order}
                    onClick={this.createSortHandler(cell.id)}
                  >
                    {cell.label}
                  </TableSortLabel>
                </Tooltip>
              </TableCell>
              ) : (
              <TableCell
                key={cell.id}
                numeric={cell.numeric}
                padding={cell.disablePadding ? 'none' : 'default'}
                className={cell.className}
              >
                {cell.label}
              </TableCell>
              )
            )
          }, this)}
        </TableRow>
      </TableHead>
    )
  }
}

EnhancedTableHead.propTypes = {
  numSelected: PropTypes.number.isRequired,
  onRequestSort: PropTypes.func.isRequired,
  onSelectAllClick: PropTypes.func.isRequired,
  order: PropTypes.string.isRequired,
  orderBy: PropTypes.string.isRequired,
  rowCount: PropTypes.number.isRequired,
  role: PropTypes.string.isRequired
}

const toolbarStyles = theme => ({
  root: {
    paddingRight: theme.spacing.unit
  },
  highlight:
    theme.palette.type === 'light'
      ? {
          color: theme.palette.secondary.main,
          backgroundColor: lighten(theme.palette.secondary.light, 0.85)
        }
      : {
          color: theme.palette.text.primary,
          backgroundColor: theme.palette.secondary.dark
        },
  spacer: {
    flex: '1 1 auto'
  },
  actions: {
    color: theme.palette.text.secondary
  },
  title: {
    flex: '0 0 auto'
  }
})

let EnhancedTableToolbar = props => {
  const { role, numSelected, classes, onCreate, onDelete, onModify } = props

  return (
    <Toolbar
      className={classNames(classes.root, {
        [classes.highlight]: role == 'admin' && numSelected > 0,
      })}
    >
      <div className={classes.title}>
        {role == 'admin' && numSelected > 0 && (
          <Typography color="inherit" variant="subheading">
            {numSelected} selected
          </Typography>
        )}
      </div>
      <div className={classes.spacer} />
      {role == 'admin' && (
          <div className={classes.actions}>
            <Tooltip title="Create">
              <IconButton aria-label="Create" onClick={onCreate}>
                <AddIcon />
              </IconButton>
            </Tooltip>
            {numSelected > 0 && (
              <Tooltip title="Delete">
                <IconButton aria-label="Delete" onClick={onDelete}>
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            )}
            {numSelected == 1 && (
              <Tooltip title="Modify">
                <IconButton aria-label="Modify" onClick={onModify}>
                  <EditIcon />
                </IconButton>
              </Tooltip>
            )}
          </div>
      )}
    </Toolbar>
  )
}

EnhancedTableToolbar.propTypes = {
  classes: PropTypes.object.isRequired,
  numSelected: PropTypes.number.isRequired,
  onDelete: PropTypes.func,
  onModify: PropTypes.func,
  role: PropTypes.string
}

EnhancedTableToolbar = withStyles(toolbarStyles)(EnhancedTableToolbar)

const styles = theme => ({
  root: {
    width: '100%'
  },
  table: {
    width: '100%'
  },
  image: {
    borderRadius: '1em'
  }
})

class EnhancedTable extends React.Component {
  handleRequestSort = (event, property) => {
    const orderBy = property
    let order = 'desc'

    if (this.props.orderBy === property && this.props.order === 'desc') {
      order = 'asc'
    }

    this.props.handleChangeOrder(order, orderBy)
  }

  handleSelectAllClick = event => {
    if (event.target.checked) {
        this.props.handleChangeSelected(this.props.designs.map(n => n.uuid))
        return
    }
    this.props.handleChangeSelected([])
  }

  handleClick = (event, id) => {
    const { selected } = this.props
    const selectedIndex = selected.indexOf(id)
    let newSelected = []

    if (selectedIndex === -1) {
      newSelected = newSelected.concat(selected, id)
    } else if (selectedIndex === 0) {
      newSelected = newSelected.concat(selected.slice(1))
    } else if (selectedIndex === selected.length - 1) {
      newSelected = newSelected.concat(selected.slice(0, -1))
    } else if (selectedIndex > 0) {
      newSelected = newSelected.concat(
        selected.slice(0, selectedIndex),
        selected.slice(selectedIndex + 1),
      )
    }

    if (this.props.role == 'admin') {
        this.props.handleChangeSelected(newSelected)
    }
  }

  handleModify = () => {
      if (this.props.selected[0]) {
          window.location = base_url + "/admin/designs/" + this.props.selected[0]
      }
  }

  isSelected = id => this.props.selected.indexOf(id) !== -1

  render() {
    const { classes, config, designs, timestamp, role, order, orderBy, selected, rowsPerPage, page } = this.props
    const emptyRows = rowsPerPage - Math.min(rowsPerPage, designs.length - page * rowsPerPage)

    return (
      <Paper className={classes.root} square={true}>
        <EnhancedTableToolbar role={role} numSelected={selected.length} onCreate={this.props.handleShowCreateDialog} onDelete={this.props.handleShowDeleteDialog} onModify={this.handleModify}/>
          <Table className={classes.table} aria-labelledby="tableTitle">
            <EnhancedTableHead
              numSelected={selected.length}
              order={order}
              orderBy={orderBy}
              onSelectAllClick={this.handleSelectAllClick}
              onRequestSort={this.handleRequestSort}
              rowCount={designs.length}
              role={role}
            />
            <TableBody>
              {stableSort(designs, getSorting(order, orderBy))
                .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
                .map(n => {
                  const isSelected = this.isSelected(n.uuid)
                  return (
                    <TableRow
                      hover={false}
                      onClick={event => this.handleClick(event, n.uuid)}
                      role="checkbox"
                      aria-checked={isSelected}
                      tabIndex={-1}
                      key={n.uuid}
                      selected={isSelected}
                    >
                      <TableCell padding="checkbox">
                        {role == 'admin' && <Checkbox checked={isSelected} />}
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <a href={"/admin/designs/" + n.uuid}><pre>{n.uuid}</pre></a>
                      </TableCell>
                      <TableCell scope="row" padding="none" className="list-image">
                        <ButtonBase
                                focusRipple
                                key={n.uuid}
                                focusVisibleClassName={classes.focusVisible}
                                style={{
                                  width: 128,
                                  height: 128,
                                  margin: '8px 0 8px 0',
                                  borderRadius: '1em'
                                }}
                              >
                            <a href={"/admin/designs/" + n.uuid}>
                                <img className={classes.image} width="128" height="128" src={config.designs_query_url + "/" + n.uuid + "/0/0/0/256.png?t=" + timestamp}/>
                            </a>
                        </ButtonBase>
                      </TableCell>
                    </TableRow>
                  )
                })}
              {emptyRows > 0 && (
                <TableRow style={{ height: 145 * emptyRows }}>
                  <TableCell colSpan={3} />
                </TableRow>
              )}
            </TableBody>
          </Table>
        <TablePagination
          component="div"
          count={designs.length}
          rowsPerPage={rowsPerPage}
          page={page}
          backIconButtonProps={{
            'aria-label': 'Previous Page',
          }}
          nextIconButtonProps={{
            'aria-label': 'Next Page',
          }}
          onChangePage={this.props.handleChangePage}
          onChangeRowsPerPage={this.props.handleChangeRowsPerPage}
        />
      </Paper>
    )
  }
}

EnhancedTable.propTypes = {
  classes: PropTypes.object.isRequired,
  config: PropTypes.object,
  designs: PropTypes.array,
  timestamp: PropTypes.number,
  role: PropTypes.string,
  order: PropTypes.string,
  orderBy: PropTypes.string,
  selected: PropTypes.array,
  page: PropTypes.number,
  rowsPerPage: PropTypes.number
}

const mapStateToProps = state => {
    console.log(JSON.stringify(state))

    return {
        config: state.designs.config,
        designs: state.designs.designs,
        timestamp: state.designs.timestamp,
        role: state.designs.account.role,
        order: state.designs.order,
        orderBy: state.designs.orderBy,
        selected: state.designs.selected,
        page: state.designs.page,
        rowsPerPage: state.designs.rowsPerPage
    }
}

const mapDispatchToProps = dispatch => ({
    handleShowDeleteDialog: () => {
        dispatch(showDeleteDesigns())
    },
    handleShowCreateDialog: () => {
        dispatch(showCreateDesign())
    },
    handleChangePage: (event, page) => {
        dispatch(setPage(page))
    },
    handleChangeRowsPerPage: (event) => {
        dispatch(setRowsPerPage(event.target.value))
    },
    handleChangeOrder: (order, orderBy) => {
        dispatch(setOrder(order, orderBy))
    },
    handleChangeSelected: (selected) => {
        dispatch(setSelected(selected))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(EnhancedTable))