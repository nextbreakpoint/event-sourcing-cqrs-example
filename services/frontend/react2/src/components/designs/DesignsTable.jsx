import React from 'react'
import { useRef, useState, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import LoadDesigns from '../../commands/loadDesigns'
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
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import UploadIcon from '@mui/icons-material/Upload'
import DownloadIcon from '@mui/icons-material/Download'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import { DataGrid, gridClasses, GridFooterContainer, GridFooter } from '@mui/x-data-grid'

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
    showErrorMessage,
    hideErrorMessage
} from '../../actions/designs'

import axios from 'axios'

export default function EnhancedTable() {
    const abortControllerRef = useRef(new AbortController())
    const config = useSelector(getConfig)
    const account = useSelector(getAccount)
    const designs = useSelector(getDesigns)
    const total = useSelector(getTotal)
    const revision = useSelector(getRevision)
    const sorting = useSelector(getSorting)
    const selection = useSelector(getSelection)
    const pagination = useSelector(getPagination)
    const dispatch = useDispatch()

    const onShowDeleteDialog = () => dispatch(showDeleteDesigns())
    const onShowCreateDialog = () => dispatch(showCreateDesign())
    const onShowUpdateDialog = () => dispatch(showUpdateDesign())
    const onShowErrorMessage = (error) => dispatch(showErrorMessage(error))
    const onHideErrorMessage = () => dispatch(hideErrorMessage())
    const onDesignSelected = (design) => dispatch(setSelectedDesign(design))
    const onLoadDesigns = () => dispatch(loadDesigns())
    const onLoadDesignsSuccess = (designs, total, revision) => dispatch(loadDesignsSuccess(designs, total, revision))
    const onChangeSorting = (sorting) => dispatch(setDesignsSorting(sorting))
    const onChangeSelection = (selection) => dispatch(setDesignsSelection(selection))
    const onChangePagination = (pagination) => dispatch(setDesignsPagination(pagination))

//   onModify = () => {
//       if (selection[0]) {
//           window.location = config.web_url + "/admin/designs/" + selection[0] + ".html"
//       }
//   }

    const onCreate = () => {
        if (selection.length == 0) {
            const script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
            const metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
            const manifest = "{\"pluginId\":\"Mandelbrot\"}"

            const design = {
                manifest: manifest,
                metadata: metadata,
                script: script
            }

            onDesignSelected(design);
        }

        if (selection.length == 1) {
            const selectedDesign = designs.find((design) => design.uuid == selection[0])
            if (selectedDesign) {
                onDesignSelected(JSON.parse(selectedDesign.json));
            }
        }

        onShowCreateDialog()
    }

    const onModify = () => {
        if (selection.length == 1) {
            const selectedDesign = designs.find((design) => design.uuid == selection[0])
            if (selectedDesign) {
                onDesignSelected(JSON.parse(selectedDesign.json));
            }

            onShowUpdateDialog()
        }
    }

    const onDelete = () => {
        onShowDeleteDialog()
    }

    const onUpload = (e) => {
        const formData = new FormData()

        formData.append('file', e.target.files[0])

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'multipart/form-data'},
            withCredentials: true
        }

        onHideErrorMessage()

        axios.post(config.api_url + '/v1/designs/upload', formData, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                    if (response.data.errors.length == 0) {
                        const design = {
                            manifest: response.data.manifest,
                            metadata: response.data.metadata,
                            script: response.data.script
                        }
                        onDesignSelected(design)
                        onChangeSelection([])
                        onShowCreateDialog()
                    } else {
                        console.log("Can't upload the file: errors = " + response.data.errors)
                        onShowErrorMessage("Can't upload the file")
                    }
                } else {
                    console.log("Can't upload the file: status = " + response.status)
                    onShowErrorMessage("Can't upload the file")
                }
            })
            .catch(function (error) {
                console.log("Can't upload the file: " + error)
                onShowErrorMessage("Can't upload the file")
            })
    }

    const onDownload = (e) => {
        if (selection[0]) {
            const axiosConfig = {
                timeout: 30000,
                metadata: {'content-type': 'application/json'},
                withCredentials: true
            }

            onHideErrorMessage()

            const uuid = selection[0]

            axios.get(config.api_url + '/v1/designs/' + uuid + '?draft=true', axiosConfig)
                .then(function (response) {
                    if (response.status == 200) {
                        console.log("Design loaded")

                        const design = JSON.parse(response.data.json)

                        const axiosConfig = {
                            timeout: 30000,
                            metadata: {'content-type': 'application/json'},
                            withCredentials: true,
                            responseType: "blob"
                        }

                        axios.post(config.api_url + '/v1/designs/download', design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 200) {
                                    const url = window.URL.createObjectURL(response.data)
                                    const a = document.createElement('a')
                                    a.href = url
                                    a.download = uuid + '.zip'
                                    a.click()
                                    onShowErrorMessage("The design has been downloaded")
                                } else {
                                    console.log("Can't download the design: status = " + response.status)
                                    onShowErrorMessage("Can't download the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't download the design: " + error)
                                onShowErrorMessage("Can't download the design")
                            })
                    } else {
                        console.log("Can't load design: status = " + content.status)
                        onShowErrorMessage("Can't load design")
                    }
                })
                .catch(function (error) {
                    console.log("Can't load design: " + error)
                    onShowErrorMessage("Can't load design")
                })
        }
    }

    const onPublish = () => {
        if (selection.length > 0) {
            const axiosConfig = {
                timeout: 30000,
                withCredentials: true
            }

            const promises = selection
                .map((uuid) => {
                    return axios.get(config.api_url + '/v1/designs/' + uuid + "?draft=true", axiosConfig)
                })

            axios.all(promises)
                .then(function (responses) {
                    const designs = responses
                        .filter((res) => {
                            return res.status == 200
                        })
                        .map((res) => {
                            return res.data
                        })

                        updateDesigns(designs, (design) => design.published = true);
                })
                .catch(function (error) {
                    console.log("Can't publish the designs: " + error)
                    onShowErrorMessage("Can't publish the designs")
                })
        }
    }

    const onUnpublish = () => {
        if (selection.length > 0) {
            const axiosConfig = {
                timeout: 30000,
                withCredentials: true
            }

            const promises = selection
                .map((uuid) => {
                    return axios.get(config.api_url + '/v1/designs/' + uuid + "?draft=true", axiosConfig)
                })

            axios.all(promises)
                .then(function (responses) {
                    const designs = responses
                        .filter((res) => {
                            return res.status == 200
                        })
                        .map((res) => {
                            return res.data
                        })

                        updateDesigns(designs, (design) => design.published = false);
                })
                .catch(function (error) {
                    console.log("Can't unpublish the designs: " + error)
                    onShowErrorMessage("Can't unpublish the designs")
                })
        }
    }

    const updateDesigns = (designs, callback) => {
        if (designs.length > 0) {
            const axiosConfig = {
                timeout: 30000,
                withCredentials: true
            }

            onHideErrorMessage()

            const promises = selection
                .map((uuid) => {
                    return designs.find((design) => design.uuid == uuid)
                })
                .filter((selectedDesign) => {
                    return selectedDesign !== undefined
                })
                .map((selectedDesign) => {
                    const design = JSON.parse(selectedDesign.json)
                    callback(design)
                    return axios.put(config.api_url + '/v1/designs/' + selectedDesign.uuid, design, axiosConfig)
                })

            axios.all(promises)
                .then(function (responses) {
                    const modifiedUuids = responses
                        .filter((res) => {
                            return (res.status == 202 || res.status == 200)
                        })
                        .map((res) => {
                            return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                        })

                    const failedUuids = responses
                        .filter((res) => {
                            return (res.status != 202 && res.status != 200)
                        })
                        .map((res) => {
                            return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                        })

                    onChangeSelection([])

                    if (failedUuids.length == 0) {
                        onShowErrorMessage("Your request has been received. The designs will be updated shortly")
                    } else {
                        console.log("Failed to unpublish designs: " + JSON.stringify(failedUuids))
                        onShowErrorMessage("Can't unpublish the designs")
                    }
                })
                .catch(function (error) {
                    console.log("Can't unpublish the designs: " + error)
                    onShowErrorMessage("Can't unpublish the designs")
                })
        }
    }

    const loadDesigns = (revision, pagination) => {
        const axiosConfig = {
            timeout: 30000,
            withCredentials: true
        }

        onLoadDesigns()

        console.log("page " + pagination.page)

        function computePercentage(design, levels) {
            const total = levels.map(i => design.tiles[i].total)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

            const completed = levels.map(i => design.tiles[i].completed)
                .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

            const percentage = Math.round((completed * 100.0) / total)

            return percentage
        }

        axios.get(config.api_url + '/v1/designs?draft=true&from=' + (pagination.page * pagination.pageSize) + '&size=' + pagination.pageSize, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    const designs = response.data.designs
                        .map((design) => {
                            return {
                                uuid: design.uuid,
                                checksum: design.checksum,
                                revision: design.revision,
                                levels: design.levels,
                                created: design.created,
                                updated: design.updated,
                                draft: design.levels != 8,
                                published: design.published,
                                percentage: computePercentage(design, [0,1,2,3,4,5,6,7]),
                                preview_percentage: computePercentage(design, [0,1,2]),
                                json: design.json
                            }
                        })
                    const total = response.data.total
                    onLoadDesignsSuccess(designs, total, revision)
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    onShowErrorMessage("Can't load designs")
                    onLoadDesignsSuccess([], 0, 0)
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                onShowErrorMessage("Can't load designs")
                onLoadDesignsSuccess([], 0, 0)
            })
    }

//   const [rowCountState, setRowCountState] = React.useState(total)
//
//   useEffect(() => {
//       setRowCountState((prevRowCountState) =>
//         total !== undefined ? total : prevRowCountState,
//       )
//   }, [total, setRowCountState])

    const EnhancedTableToolbar = () => {
        return (
            <Toolbar className="designs-toolbar">
            {/*       <div className="title"> */}
            {/*         {account.role == 'admin' && selection.length > 0 && ( */}
            {/*           <Typography color="inherit" variant="subheading"> */}
            {/*             {selection.length} {selection.length == 1 ? "row" : "rows"} selected */}
            {/*           </Typography> */}
            {/*         )} */}
            {/*       </div> */}
            {/*       <div className="spacer" /> */}
            {account.role == 'admin' && (
                <div className="toolbar-actions">
                    <Tooltip title="Upload">
                        <label htmlFor="uploadFile">
                        <Input className="upload-file" id="uploadFile" accept="application/zip" type="file" onChange={onUpload} />
                        <IconButton aria-label="Upload" component="span">
                            <UploadIcon />
                        </IconButton>
                        </label>
                    </Tooltip>
                    {selection.length == 1 && (
                        <Tooltip title="Download">
                            <IconButton aria-label="Download" onClick={onDownload}>
                                <DownloadIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                    <Tooltip title="Create">
                        <IconButton aria-label="Create" onClick={onCreate}>
                            <AddIcon />
                        </IconButton>
                    </Tooltip>
                    {selection.length == 1 && (
                        <Tooltip title="Modify">
                            <IconButton aria-label="Modify" onClick={onModify}>
                                <EditIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                    {selection.length > 0 && (
                        <Tooltip title="Delete">
                            <IconButton aria-label="Delete" onClick={onDelete}>
                                <DeleteIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                    {selection.length > 0 && (
                        <Tooltip title="Publish">
                            <IconButton aria-label="Publish" onClick={onPublish}>
                                <VisibilityIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                    {selection.length > 0 && (
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

    const CustomFooter = () => {
        return (
            <GridFooterContainer className={classNames({["highlight"]: account.role == 'admin' && selection.length > 0 })}>
                <EnhancedTableToolbar/>
                <GridFooter sx={{ border: 'none' }}/>
            </GridFooterContainer>
        );
    }

    const getData = (designs) => {
        return designs
            .map(design => {
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
    }

    return (
        <Paper className="designs" square={true}>
        <div class="spacer"/>
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
            rows={getData(designs)}
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
                onChangeSelection(selection);

                if (selection.length == 1) {
                    const selectedDesign = designs.find((design) => design.uuid == selection[0])
                    if (selectedDesign) {
                        onDesignSelected(JSON.parse(selectedDesign.json));
                    }
                }
            }}
            paginationMode="server"
            paginationModel={pagination}
            onPaginationModelChange={(pagination) => {
              onChangePagination(pagination)

              loadDesigns(revision, pagination)
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
