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
import { DataGrid, gridClasses, GridFooterContainer, GridFooter } from '@mui/x-data-grid'

import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import UploadIcon from '@mui/icons-material/ArrowUpward'
import DownloadIcon from '@mui/icons-material/ArrowDownward'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount
} from '../../actions/account'

import {
    showCreateDesign,
    showUpdateDesign,
    showDeleteDesigns,
    setDesignsSorting,
    setDesignsSelection,
    setDesignsPagination,
    setSelectedDesign,
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
    hideErrorMessage
} from '../../actions/designs'

import axios from 'axios'

let EnhancedTableToolbar = props => {
  const { role, numSelected, onDownload, onUpload, onCreate, onDelete, onModify, onPublish, onUnpublish } = props

  return (
    <Toolbar className="designs-toolbar">
{/*       <div className="title"> */}
{/*         {role == 'admin' && numSelected > 0 && ( */}
{/*           <Typography color="inherit" variant="subheading"> */}
{/*             {numSelected} {numSelected == 1 ? "row" : "rows"} selected */}
{/*           </Typography> */}
{/*         )} */}
{/*       </div> */}
{/*       <div className="spacer" /> */}
      {role == 'admin' && (
          <div className="toolbar-actions">
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
            {numSelected == 1 && (
              <Tooltip title="Modify">
                <IconButton aria-label="Modify" onClick={onModify}>
                  <EditIcon />
                </IconButton>
              </Tooltip>
            )}
            {numSelected > 0 && (
              <Tooltip title="Delete">
                <IconButton aria-label="Delete" onClick={onDelete}>
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            )}
            {numSelected > 0 && (
              <Tooltip title="Publish">
                <IconButton aria-label="Publish" onClick={onPublish}>
                  <VisibilityIcon />
                </IconButton>
              </Tooltip>
            )}
            {numSelected > 0 && (
              <Tooltip title="Unpublish">
                <IconButton aria-label="Unpublish" onClick={onUnpublish}>
                  <VisibilityOffIcon />
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
  onPublish: PropTypes.func,
  onUnpublish: PropTypes.func,
  role: PropTypes.string
}

let EnhancedTable = class EnhancedTable extends React.Component {
//   handleModify = () => {
//       if (this.props.selection[0]) {
//           window.location = this.props.config.web_url + "/admin/designs/" + this.props.selection[0] + ".html"
//       }
//   }

  handleCreate = () => {
    console.log("create")

    if (this.props.selection.length == 0) {
        let default_script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
        let default_metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
        let default_manifest = "{\"pluginId\":\"Mandelbrot\"}"
        let design = {manifest: default_manifest, metadata: default_metadata, script: default_script}
        this.props.handleDesignSelected(design);
    }

    if (this.props.selection.length == 1) {
        let selectedDesign = this.props.designs.find((design) => design.uuid == this.props.selection[0])
        if (selectedDesign) {
            let design = JSON.parse(selectedDesign.json)
            this.props.handleDesignSelected(design);
        }
    }

    this.props.handleShowCreateDialog()
  }

  handleModify = () => {
    if (this.props.selection.length == 1) {
        console.log("modify")

        let selectedDesign = this.props.designs.find((design) => design.uuid == this.props.selection[0])
        if (selectedDesign) {
            let design = JSON.parse(selectedDesign.json)
            this.props.handleDesignSelected(design);
        }

        this.props.handleShowUpdateDialog()
    }
  }

  handleDelete = () => {
    console.log("delete")

    this.props.handleShowDeleteDialog()
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
                    component.props.handleDesignSelected(design)
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

    handlePublish = () => {
        if (this.props.selection.length > 0) {
            console.log("publish")

            let component = this

            let config = {
                timeout: 30000,
                withCredentials: true
            }

            let promises = this.props.selection
                .map((uuid) => {
                    let selectedDesign = this.props.designs.find((design) => design.uuid == this.props.selection[0])
                    if (selectedDesign) {
                        let design = JSON.parse(selectedDesign.json)
                        design.published = true
                        return axios.put(component.props.config.api_url + '/v1/designs/' + selectedDesign.uuid, design, config)
                    }
                })

            component.props.handleHideErrorMessage()

            axios.all(promises)
                .then(function (responses) {
                    let modifiedUuids = responses
                        .filter((res) => {
                            return (res.status == 202 || res.status == 200)
                        })
                        .map((res) => {
                            return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                        })

                    let failedUuids = responses
                        .filter((res) => {
                            return (res.status != 202 && res.status != 200)
                        })
                        .map((res) => {
                            return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                        })

                    let designs = component.props.designs
                        .filter((design) => {
                            return !modifiedUuids.includes(design.uuid)
                        })
                        .map((design) => {
                            return { uuid: design.uuid, selected: design.selected }
                        })

                    if (failedUuids.length == 0) {
                        component.props.handleShowErrorMessage("Your request has been received. The designs will be updated shortly")
                    } else {
                        component.props.handleShowErrorMessage("Can't update the designs")
                    }
                })
                .catch(function (error) {
                    console.log("Can't update the designs: " + error)
                    component.props.handleShowErrorMessage("Can't update the designs")
                })
        }
    }

    handleUnpublish = () => {
        if (this.props.selection.length > 0) {
            console.log("unpublish")

            let component = this

            let config = {
                timeout: 30000,
                withCredentials: true
            }

            let promises = this.props.selection
                .map((uuid) => {
                    let selectedDesign = this.props.designs.find((design) => design.uuid == this.props.selection[0])
                    if (selectedDesign) {
                        let design = JSON.parse(selectedDesign.json)
                        design.published = false
                        return axios.put(component.props.config.api_url + '/v1/designs/' + selectedDesign.uuid, design, config)
                    }
                })

            component.props.handleHideErrorMessage()

            axios.all(promises)
                .then(function (responses) {
                    let modifiedUuids = responses
                        .filter((res) => {
                            return (res.status == 202 || res.status == 200)
                        })
                        .map((res) => {
                            return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                        })

                    let failedUuids = responses
                        .filter((res) => {
                            return (res.status != 202 && res.status != 200)
                        })
                        .map((res) => {
                            return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                        })

                    let designs = component.props.designs
                        .filter((design) => {
                            return !modifiedUuids.includes(design.uuid)
                        })
                        .map((design) => {
                            return { uuid: design.uuid, selected: design.selected }
                        })

                    if (failedUuids.length == 0) {
                        component.props.handleShowErrorMessage("Your request has been received. The designs will be updated shortly")
                    } else {
                        component.props.handleShowErrorMessage("Can't update the designs")
                    }
                })
                .catch(function (error) {
                    console.log("Can't update the designs: " + error)
                    component.props.handleShowErrorMessage("Can't update the designs")
                })
        }
    }

  loadDesigns = (revision, pagination) => {
    console.log("Load designs")

    let component = this

    let config = {
        timeout: 30000,
        withCredentials: true
    }

    component.props.handleLoadDesigns()

    console.log("page " + pagination.page)

    function computePercentage(design, levels) {
        let total = levels.map(i => design.tiles[i].total)
            .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

        let completed = levels.map(i => design.tiles[i].completed)
            .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

        let percentage = Math.round((completed * 100.0) / total)

        return percentage
    }

    axios.get(component.props.config.api_url + '/v1/designs?draft=true&from=' + (pagination.page * pagination.pageSize) + '&size=' + pagination.pageSize, config)
        .then(function (response) {
            if (response.status == 200) {
                console.log("Designs loaded")
                let designs = response.data.designs.map((design) => { return { uuid: design.uuid, checksum: design.checksum, revision: design.revision, levels: design.levels, created: design.created, updated: design.updated, draft: design.levels != 8, published: design.published, percentage: computePercentage(design, [0,1,2,3,4,5,6,7]), preview_percentage: computePercentage(design, [0,1,2]) }})
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

//   let [rowCountState, setRowCountState] = React.useState(this.props.total)
//
//   React.useEffect(() => {
//       setRowCountState((prevRowCountState) =>
//         rowCount !== undefined ? rowCount : prevRowCountState,
//       )
//   }, [this.props.total, setRowCountState])

  render() {
    const { config, designs, account, revision, sorting, selection, pagination, total } = this.props

    let CustomFooter = () => {
        return (
            <GridFooterContainer className={classNames({["highlight"]: account.role == 'admin' && selection.length > 0 })}>
                <EnhancedTableToolbar role={account.role} numSelected={selection.length} onDownload={this.handleDownload} onUpload={this.handleUpload} onCreate={this.handleCreate} onModify={this.handleModify} onDelete={this.handleDelete} onPublish={this.handlePublish} onUnpublish={this.handleUnpublish}/>
                <GridFooter sx={{
                    border: 'none'
                }} />
            </GridFooterContainer>
        );
    }

    let rows = designs.map(design => {
       return {
            id: design.uuid,
            image: { url: config.api_url + "/v1/designs/" + design.uuid + "/0/0/0/256.png?draft=true&t=" + design.checksum + "&r=" + design.preview_percentage, uuid: design.uuid },
            uuid: design.uuid,
            created: new Date(design.created),
            updated: new Date(design.updated),
            draft: design.draft,
            published: design.published,
            percentage: design.percentage
        }
    })

    return (
      <Paper className="designs" square={true}>
        <DataGrid
            components={{Footer: CustomFooter}}
            rowCount={total}
            columns={[
                {
                    field: 'image',
                    type: 'string',
                    headerName: 'Image',
                    sortable: false,
                    hideable: false,
                    filterable: false,
                    minWidth: 300,
                    renderCell: (params) => <a class="image" href={"/admin/designs/" + params.value.uuid + ".html"}><img src={params.value.url} /></a>
                },
                {
                    field: 'uuid',
                    type: 'string',
                    headerName: 'UUID',
                    hideable: false,
                    flex: 1.0,
                    renderCell: (params) => <a class="link" href={"/admin/designs/" + params.value + ".html"}>{params.value}</a>
                },
                {
                    field: 'created',
                    type: 'dataTime',
                    headerName: 'Created',
                    hideable: false,
                    flex: 0.8,
                    renderCell: (params) => <span>{params.value.toLocaleString('en-GB', { timeZone: 'UTC' })}</span>
                },
                {
                    field: 'updated',
                    type: 'dataTime',
                    headerName: 'Updated',
                    hideable: false,
                    flex: 0.8,
                    renderCell: (params) => <span>{params.value.toLocaleString('en-GB', { timeZone: 'UTC' })}</span>
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
                    type: 'number',
                    headerName: 'Percentage',
                    hideable: false,
                    flex: 0.5,
                    renderCell: (params) => <span>{params.value + "%"}</span>
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
//             loading={isLoading}
            checkboxSelection
            disableRowSelectionOnClick
            keepNonExistentRowsSelected
            rowSelectionModel={selection}
            onRowSelectionModelChange={(selection) => {
              this.props.handleChangeSelection(selection);
              if (selection.length == 1) {
                let selectedDesign = designs.find((design) => design.uuid == selection[0])
                if (selectedDesign) {
                    let design = JSON.parse(selectedDesign.json)
                    this.props.handleDesignSelected(design);
                }
              }
            }}
            paginationMode="server"
            paginationModel={pagination}
            onPaginationModelChange={(pagination) => {
              this.props.handleChangePagination(pagination)
              this.loadDesigns(revision, pagination)
            }}
            pageSizeOptions={[5, 10, 20]}
            sx={{
                display: "flex",
                flexDirection: "column-reverse",
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
    handleShowUpdateDialog: () => {
        dispatch(showUpdateDesign())
    },
    handleShowErrorMessage: (error) => {
        dispatch(showErrorMessage(error))
    },
    handleHideErrorMessage: () => {
        dispatch(hideErrorMessage())
    },
    handleDesignSelected: (design) => {
        dispatch(setSelectedDesign(design))
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
