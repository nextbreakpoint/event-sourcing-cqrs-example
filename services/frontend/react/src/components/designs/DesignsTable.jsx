import React from 'react'
import PropTypes from 'prop-types'
import classNames from 'classnames'
import FormData from 'form-data'

import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TableSortLabel from '@mui/material/TableSortLabel'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import Paper from '@mui/material/Paper'
import Checkbox from '@mui/material/Checkbox'
import IconButton from '@mui/material/IconButton'
import ButtonBase from '@mui/material/ButtonBase'
import Tooltip from '@mui/material/Tooltip'
import Input from '@mui/material/Input'
import { DataGrid, gridClasses } from '@mui/x-data-grid'

import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import UploadIcon from '@mui/icons-material/ArrowUpward'
import DownloadIcon from '@mui/icons-material/ArrowDownward'

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
    getSorting,
    getSelection,
    getPagination,
    loadDesigns,
    loadDesignsSuccess,
    getShowErrorMessage,
    getErrorMessage,
    showErrorMessage,
    hideErrorMessage,
    setUploadedDesign
} from '../../actions/designs'

import axios from 'axios'

let EnhancedTableToolbar = props => {
  const { role, numSelected, onDownload, onUpload, onCreate, onDelete, onModify } = props

  return (
    <Toolbar className={classNames("toolbar", {["highlight"]: role == 'admin' && numSelected > 0 })}>
      <div className="title">
        {role == 'admin' && numSelected > 0 && (
          <Typography color="inherit" variant="subheading">
            {numSelected} {numSelected == 1 ? "row" : "rows"} selected
          </Typography>
        )}
      </div>
      <div className="spacer" />
      {role == 'admin' && (
          <div className="actions">
            <Tooltip title="Upload">
              <label htmlFor="uploadFile">
                  <Input className="upload-file" id="uploadFile" accept="application/zip" type="file" onChange={onUpload} />
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
  numSelected: PropTypes.number.isRequired,
  onUpload: PropTypes.func,
  onCreate: PropTypes.func,
  onDelete: PropTypes.func,
  onModify: PropTypes.func,
  role: PropTypes.string
}

let EnhancedTable = class EnhancedTable extends React.Component {
  handleModify = () => {
      if (this.props.selection[0]) {
          window.location = this.props.config.web_url + "/admin/designs/" + this.props.selection[0] + ".html"
      }
  }

  handleUpload = (e) => {
    console.log("upload")

    let component = this

    let formData = new FormData()
    formData.append('file', e.target.files[0])

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
                    console.log("Can't upload the file: errors = " + response.data.errors)
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
      if (this.props.selection[0]) {
        console.log("download")

        let component = this

        let uuid = this.props.selection[0]

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
                                let url = window.URL.createObjectURL(response.data)
                                let a = document.createElement('a')
                                a.href = url
                                a.download = uuid + '.zip'
                                a.click()
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

//   loadDesigns = (pagination, revision) => {
//     console.log("Load designs")
//
//     let component = this
//
//     let config = {
//         timeout: 30000,
//         withCredentials: true
//     }
//
//     component.props.handleLoadDesigns()
//     component.props.handleChangePagination(pagination)
//
//     console.log("page " + pagination.page)
//
//     function computePercentage(design, levels) {
//         let total = levels.map(i => design.tiles[i].total)
//             .reduce((previousValue, currentValue) => previousValue + currentValue, 0)
//
//         let completed = levels.map(i => design.tiles[i].completed)
//             .reduce((previousValue, currentValue) => previousValue + currentValue, 0)
//
//         let percentage = Math.round((completed * 100.0) / total)
//
//         return percentage
//     }
//
//     axios.get(component.props.config.api_url + '/v1/designs?draft=true&from=' + (pagination.page * pagination.pageSize) + '&size=' + pagination.pageSize, config)
//         .then(function (response) {
//             if (response.status == 200) {
//                 console.log("Designs loaded")
//                 let designs = response.data.designs.map((design) => { return { uuid: design.uuid, checksum: design.checksum, revision: design.revision, levels: design.levels, created: design.created, updated: design.updated, draft: design.levels != 8, published: design.published, percentage: computePercentage(design, [0,1,2,3,4,5,6,7]), preview_percentage: computePercentage(design, [0,1,2]) }})
//                 let total = response.data.total
//                 component.props.handleLoadDesignsSuccess(designs, total, revision)
//             } else {
//                 console.log("Can't load designs: status = " + content.status)
//                 component.props.handleLoadDesignsSuccess([], 0, 0)
//                 component.props.handleShowErrorMessage("Can't load designs")
//             }
//         })
//         .catch(function (error) {
//             console.log("Can't load designs " + error)
//             component.props.handleLoadDesignsSuccess([], 0, 0)
//             component.props.handleShowErrorMessage("Can't load designs")
//         })
//   }

//   isSelected = id => this.props.selection.indexOf(id) !== -1

//     let [rowCountState, setRowCountState] = React.useState(rowCount)
//     React.useEffect(() => {
//       setRowCountState((prevRowCountState) =>
//         rowCount !== undefined ? rowCount : prevRowCountState,
//       )
//     }, [rowCount, setRowCountState])


// /admin/designs/" + design.uuid + '.html'

  render() {
    const { config, designs, account, sorting, selection, pagination, total } = this.props

    let rows = designs.map(design => {
       return {
            id: design.uuid,
            image: config.api_url + "/v1/designs/" + design.uuid + "/0/0/0/256.png?draft=true&t=" + design.checksum + "&r=" + design.preview_percentage,
            uuid: design.uuid,
            created: design.created,
            updated: design.updated,
            draft: design.draft,
            published: design.published,
            percentage: design.percentage + '%'
        }
    })

{/* <DataGrid */}
{/*   loading={isLoading} */}
{/*   paginationModel={paginationModel} */}
{/*   paginationMode="server" */}
{/*   onPaginationModelChange={setPaginationModel} */}
{/* /> */}
    return (
      <Paper className="designs" square={true}>
        <EnhancedTableToolbar role={account.role} numSelected={selection.length} onDownload={this.handleDownload} onUpload={this.handleUpload} onCreate={this.props.handleShowCreateDialog} onDelete={this.props.handleShowDeleteDialog} onModify={this.handleModify}/>
        <DataGrid
            rowCount={total}
            columns={[
                {
                    field: 'image',
                    type: 'string',
                    headerName: 'Image',
                    sortable: false,
                    hideable: false,
                    minWidth: 300,
                    renderCell: (params) => <img src={params.value} />
                },
                {
                    field: 'uuid',
                    type: 'string',
                    headerName: 'UUID',
                    hideable: false,
                    flex: 1.5,
                    renderCell: (params) => <a href={"/admin/designs/" + params.value + ".html"}>{params.value}</a>
                },
                {
                    field: 'created',
                    type: 'string',
                    headerName: 'Created',
                    hideable: false,
                    flex: 1
                },
                {
                    field: 'updated',
                    type: 'string',
                    headerName: 'Updated',
                    hideable: false,
                    flex: 1
                },
                {
                    field: 'draft',
                    type: 'boolean',
                    headerName: 'Draft',
                    hideable: false,
                    flex: 0.5
                },
                {
                    field: 'published',
                    type: 'boolean',
                    headerName: 'Published',
                    hideable: false,
                    flex: 0.5
                },
                {
                    field: 'percentage',
                    type: 'string',
                    headerName: 'Percentage',
                    hideable: false,
                    flex: 0.5
                }
            ]}
            rows={rows}
            initialState={{
                pagination: {
                    paginationModel: pagination
                },
                sorting: {
                  sortModel: sorting
                }
            }}
            autoHeight
            getRowHeight={() => 'auto'}
            getEstimatedRowHeight={() => 256}
            checkboxSelection
            disableRowSelectionOnClick
            keepNonExistentRowsSelected
            rowSelectionModel={selection}
            onRowSelectionModelChange={(selection) => {
              this.props.handleChangeSelection(selection);
            }}
            paginationModel={pagination}
            onPaginationModelChange={(pagination) => {
              this.props.handleChangePagination(pagination);
            }}
            sx={{
              [`& .${gridClasses.cell}:focus, & .${gridClasses.cell}:focus-within`]: {
                outline: 'none'
              },
              [`& .${gridClasses.columnHeader}:focus, & .${gridClasses.columnHeader}:focus-within`]: {
                outline: 'none'
              }
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
    sorting: PropTypes.array,
    selection: PropTypes.array,
    pagination: PropTypes.object
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    total: getTotal(state),
    revision: getRevision(state),
    sorting: getSorting(state),
    selection: getSelection(state),
    pagination: getPagination(state)
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
    handleUploadedDesign: (design) => {
        dispatch(setUploadedDesign(design))
    },
    handleLoadDesigns: () => {
        dispatch(loadDesigns())
    },
    handleLoadDesignsSuccess: (designs, total, revision) => {
        dispatch(loadDesignsSuccess(designs, total, revision))
    },
    handleChangeSorting: (sorting) => {
        dispatch(setDesignsSorting(sorting))
    },
    handleChangeSelection: (selection) => {
        dispatch(setDesignsSelection(selection))
    },
    handleChangePagination: (pagination) => {
        dispatch(setDesignsPagination(pagination))
    }
})

export default connect(mapStateToProps, mapDispatchToProps)(EnhancedTable)
