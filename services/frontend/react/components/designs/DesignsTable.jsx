import React from 'react'
import PropTypes from 'prop-types'
import classNames from 'classnames'
import FormData from 'form-data'

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
import Input from '@material-ui/core/Input'

import AddIcon from '@material-ui/icons/Add'
import EditIcon from '@material-ui/icons/Edit'
import DeleteIcon from '@material-ui/icons/Delete'
import UploadIcon from '@material-ui/icons/ArrowUpward'
import DownloadIcon from '@material-ui/icons/ArrowDownward'

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount
} from '../../actions/account'

import {
    showCreateDesign,
    showDeleteDesigns,
    setDesignsSorting,
    setDesignsSelection,
    setDesignsPagination,
    getTotal,
    getDesigns,
    getRevision,
    getSelected,
    getOrder,
    getOrderBy,
    loadDesigns,
    getPage,
    getRowsPerPage,
    loadDesignsSuccess,
    loadDesignsFailure,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage,
    setUploadedDesign
} from '../../actions/designs'

import axios from 'axios'

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

function stableSort(array, comparator) {
  const stabilizedThis = array.map((el, index) => [el, index])
  stabilizedThis.sort((a, b) => {
    const order = comparator(a[0], b[0])
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
  { id: 'created', numeric: false, disablePadding: true, label: 'Created', enableSort: true, className: '' },
  { id: 'updated', numeric: false, disablePadding: true, label: 'Updated', enableSort: true, className: '' },
  { id: 'checksum', numeric: false, disablePadding: true, label: 'Checksum', enableSort: true, className: '' },
  { id: 'draft', numeric: true, disablePadding: true, label: 'Draft', enableSort: true, className: '' },
  { id: 'published', numeric: false, disablePadding: true, label: 'Published', enableSort: true, className: '' },
  { id: 'percentage', numeric: true, disablePadding: true, label: 'Progress', enableSort: false, className: '' },
  { id: 'image', numeric: false, disablePadding: true, label: '', enableSort: false, className: 'list-image' }
]

let EnhancedTableHead = class EnhancedTableHead extends React.Component {
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
  },
  uploadFile: {
    display: 'none'
  }
})

let EnhancedTableToolbar = props => {
  const { role, numSelected, classes, onDownload, onUpload, onCreate, onDelete, onModify } = props

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
            <Tooltip title="Upload">
              <label htmlFor="uploadFile">
                  <Input className={classes.uploadFile} id="uploadFile" accept="application/zip" type="file" onChange={onUpload} />
                  <IconButton aria-label="Upload" component="span">
                    <UploadIcon />
                  </IconButton>
              </label>
            </Tooltip>
            <Tooltip title="Download">
              <IconButton aria-label="Download" onClick={onDownload} disabled={numSelected != 1}>
                <DownloadIcon />
              </IconButton>
            </Tooltip>
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
  onUpload: PropTypes.func,
  onCreate: PropTypes.func,
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

let EnhancedTable = class EnhancedTable extends React.Component {
  handleRequestSort = (event, property) => {
    const orderBy = property
    let order = 'desc'

    if (this.props.orderBy === property && this.props.order === 'desc') {
      order = 'asc'
    }

    this.props.handleChangeSorting(order, orderBy)
  }

  handleSelectAllClick = event => {
    if (event.target.checked) {
        this.props.handleChangeSelection(this.props.designs.map(n => n.uuid))
        return
    }
    this.props.handleChangeSelection([])
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

    if (this.props.account.role == 'admin') {
        this.props.handleChangeSelection(newSelected)
    }
  }

  handleModify = () => {
      if (this.props.selected[0]) {
          window.location = this.props.config.web_url + "/admin/designs/" + this.props.selected[0] + ".html"
      }
  }

  handleUpload = (e) => {
    console.log("upload")

    let component = this

    let formData = new FormData();
    formData.append('file', e.target.files[0]);

    let config = {
        timeout: 30000,
        metadata: {'content-type': 'multipart/form-data'},
        withCredentials: true
    }

    component.props.handleHideErrorMessage()

    axios.post(component.props.config.api_url + '/v1/designs/upload', formData, config)
        .then(function (response) {
            if (response.status == 200) {
                if (response.data.errors.length == 0) {
                    let design = { manifest: response.data.manifest, metadata: response.data.metadata, script: response.data.script }
                    component.props.handleUploadedDesign(design)
                    component.props.handleShowCreateDialog()
                } else {
                    component.props.handleShowErrorMessage("Can't upload the file")
                }
            } else {
                console.log("Can't upload the file: status = " + response.status)
                component.props.handleShowErrorMessage("Can't upload the file")
            }
        })
        .catch(function (error) {
            console.log("Can't upload the file: " + error)
            component.props.handleShowErrorMessage("Can't upload the file")
        })
  }

  handleDownload = (e) => {
      if (this.props.selected[0]) {
        console.log("download")

        let component = this

        let uuid = this.props.selected[0]

        let config = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        component.props.handleHideErrorMessage()

        axios.get(component.props.config.api_url + '/v1/designs/' + uuid + '?draft=true', config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Design loaded")

                    let design = JSON.parse(response.data.json)

                    let config = {
                        timeout: 30000,
                        metadata: {'content-type': 'application/json'},
                        withCredentials: true,
                        responseType: "blob"
                    }

                    axios.post(component.props.config.api_url + '/v1/designs/download', design, config)
                        .then(function (response) {
                            if (response.status == 200) {
                                let url = window.URL.createObjectURL(response.data);
                                let a = document.createElement('a');
                                a.href = url;
                                a.download = uuid + '.zip';
                                a.click();
                                component.props.handleShowErrorMessage("The design has been downloaded")
                            } else {
                                console.log("Can't download the design: status = " + response.status)
                                component.props.handleShowErrorMessage("Can't download the design")
                            }
                        })
                        .catch(function (error) {
                            console.log("Can't download the design: " + error)
                            component.props.handleShowErrorMessage("Can't download the design")
                        })
                } else {
                    console.log("Can't load design: status = " + content.status)
                    component.props.handleShowErrorMessage("Can't load design")
                }
            })
            .catch(function (error) {
                console.log("Can't load design: " + error)
                component.props.handleShowErrorMessage("Can't load design")
            })
      }
  }

    loadDesigns = (page, rowsPerPage, revision) => {
        console.log("Load designs")

        let component = this

        let config = {
            timeout: 30000,
            withCredentials: true
        }

        component.props.handleLoadDesigns()
        component.props.handleChangePagination(page, rowsPerPage)

        console.log("page " + page)

        function computePercentage(design) {
            const levels = [0,1,2,3,4,5,6,7];
            let total = levels.map(i => design.tiles[i].total)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)
            let completed = levels.map(i => design.tiles[i].completed)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)
            let percentage = Math.round((completed * 100.0) / total)
            console.log("uuid = " + design.uuid + ", percentage = " + percentage)
            return percentage
        }

        axios.get(component.props.config.api_url + '/v1/designs?draft=true&from=' + (page * rowsPerPage) + '&size=' + rowsPerPage, config)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    let designs = response.data.designs.map((design) => { return { uuid: design.uuid, checksum: design.checksum, revision: design.revision, levels: design.levels, created: design.created, updated: design.updated, draft: design.levels != 8, published: design.published, percentage: computePercentage(design) }})
                    let total = response.data.total
                    component.props.handleLoadDesignsSuccess(designs, total, revision)
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    component.props.handleLoadDesignsSuccess([], 0, 0)
                    component.props.handleShowErrorMessage("Can't load designs")
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                component.props.handleLoadDesignsSuccess([], 0, 0)
                component.props.handleShowErrorMessage("Can't load designs")
            })
    }

  isSelected = id => this.props.selected.indexOf(id) !== -1

  render() {
    const { classes, config, designs, account, order, orderBy, selected, rowsPerPage, page, total } = this.props
    const emptyRows = 0 //rowsPerPage - Math.min(rowsPerPage, designs.length - page * rowsPerPage)

    return (
      <Paper className={classes.root} square={true}>
        <EnhancedTableToolbar role={account.role} numSelected={selected.length} onDownload={this.handleDownload} onUpload={this.handleUpload} onCreate={this.props.handleShowCreateDialog} onDelete={this.props.handleShowDeleteDialog} onModify={this.handleModify}/>
          <Table className={classes.table} aria-labelledby="tableTitle">
            <EnhancedTableHead
              numSelected={selected.length}
              order={order}
              orderBy={orderBy}
              onSelectAllClick={this.handleSelectAllClick}
              onRequestSort={this.handleRequestSort}
              rowCount={designs.length}
              role={account.role}
            />
            <TableBody>
              {designs.map(design => {
                  const isSelected = this.isSelected(design.uuid)
                  return (
                    <TableRow
                      hover={false}
                      onClick={event => this.handleClick(event, design.uuid)}
                      role="checkbox"
                      aria-checked={isSelected}
                      tabIndex={-1}
                      key={design.uuid}
                      selected={isSelected}
                    >
                      <TableCell padding="checkbox">
                        {account.role == 'admin' && <Checkbox checked={isSelected} />}
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <a href={"/admin/designs/" + design.uuid + '.html'}><pre>{design.uuid}</pre></a>
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <pre>{design.created}</pre>
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <pre>{design.updated}</pre>
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <pre>{design.checksum}</pre>
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <pre>{design.draft ? 'yes' : 'no'}</pre>
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <pre>{design.published ? 'yes' : 'no'}</pre>
                      </TableCell>
                      <TableCell scope="row" padding="none">
                        <pre>{design.percentage}%</pre>
                      </TableCell>
                      <TableCell scope="row" padding="none" className="list-image">
                        <ButtonBase
                                focusRipple
                                key={design.uuid}
                                focusVisibleClassName={classes.focusVisible}
                                style={{
                                  width: 256,
                                  height: 256,
                                  margin: '8px 0 8px 0',
                                  borderRadius: '1em'
                                }}
                              >
                            <a href={"/admin/designs/" + design.uuid + ".html"}>
                                <img className={classes.image} width="256" height="256" src={config.api_url + "/v1/designs/" + design.uuid + "/0/0/0/256.png?draft=true&t=" + design.checksum}/>
                            </a>
                        </ButtonBase>
                      </TableCell>
                    </TableRow>
                  )
                })}
              {emptyRows > 0 && (
                <TableRow style={{ height: 300 * emptyRows }}>
                  <TableCell colSpan={3} />
                </TableRow>
              )}
            </TableBody>
          </Table>
        <TablePagination
          component="div"
          count={total}
          rowsPerPage={5}
          rowsPerPageOptions={[5]}
          page={page}
          backIconButtonProps={{
            'aria-label': 'Previous Page',
          }}
          nextIconButtonProps={{
            'aria-label': 'Next Page',
          }}
          onPageChange={(event, value) => {
            this.loadDesigns(value, this.props.rowsPerPage, "0")
          }}
          onRowsPerPageChange={event => {
            this.loadDesigns(this.props.page, event.target.value, "0")
          }}
        />
      </Paper>
    )
  }
}

EnhancedTable.propTypes = {
    config: PropTypes.object,
    account: PropTypes.object,
    total: PropTypes.number,
    designs: PropTypes.array,
    revision: PropTypes.number,
    selected: PropTypes.array,
    order: PropTypes.string,
    orderBy: PropTypes.string,
    page: PropTypes.number,
    rowsPerPage: PropTypes.number,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    total: getTotal(state),
    revision: getRevision(state),
    selected: getSelected(state),
    order: getOrder(state),
    orderBy: getOrderBy(state),
    page: getPage(state),
    rowsPerPage: getRowsPerPage(state)
})

const mapDispatchToProps = dispatch => ({
    handleShowDeleteDialog: () => {
        dispatch(showDeleteDesigns())
    },
    handleShowCreateDialog: () => {
        dispatch(showCreateDesign())
    },
    handleShowErrorMessage: (error) => {
        dispatch(showErrorMessage(error))
    },
    handleHideErrorMessage: () => {
        dispatch(hideErrorMessage())
    },
    handleLoadDesigns: () => {
        dispatch(loadDesigns())
    },
    handleLoadDesignsSuccess: (designs, revision) => {
        dispatch(loadDesignsSuccess(designs, revision))
    },
    handleLoadDesignsFailure: (error) => {
        dispatch(loadDesignsFailure(error))
    },
    handleChangePagination: (page, rowsPerPage) => {
        dispatch(setDesignsPagination(page, rowsPerPage))
    },
    handleChangeSorting: (order, orderBy) => {
        dispatch(setDesignsSorting(order, orderBy))
    },
    handleChangeSelection: (selected) => {
        dispatch(setDesignsSelection(selected))
    },
    handleUploadedDesign: (design) => {
        dispatch(setUploadedDesign(design))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(EnhancedTable))
