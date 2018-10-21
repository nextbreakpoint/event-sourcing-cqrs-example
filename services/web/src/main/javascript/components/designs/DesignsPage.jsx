import React from 'react'
import PropTypes from 'prop-types'

import Header from '../shared/Header'
import Footer from '../shared/Footer'
import DesignForm from '../shared/DesignForm'
import DesignsTable from './DesignsTable'

import { withStyles } from '@material-ui/core/styles'

import CssBaseline from '@material-ui/core/CssBaseline'
import Button from '@material-ui/core/Button'
import Grid from '@material-ui/core/Grid'
import Dialog from '@material-ui/core/Dialog'
import DialogActions from '@material-ui/core/DialogActions'
import DialogContent from '@material-ui/core/DialogContent'
import DialogTitle from '@material-ui/core/DialogTitle'
import Slide from '@material-ui/core/Slide'
import Fade from '@material-ui/core/Fade'

import AddIcon from '@material-ui/icons/Add'
import EditIcon from '@material-ui/icons/Edit'
import DeleteIcon from '@material-ui/icons/Delete'
import CloseIcon from '@material-ui/icons/Close'

import { connect } from 'react-redux'

import {
    getConfig
} from '../../actions/config'

import {
    getAccount
} from '../../actions/account'

import {
    getSelected,
    getDesigns,
    getTimestamp,
    getShowCreateDesign,
    getShowDeleteDesigns,
    showDeleteDesigns,
    hideDeleteDesigns,
    showCreateDesign,
    hideCreateDesign,
    setDesignsSelection,
    loadDesignsSuccess
} from '../../actions/designs'

import axios from 'axios'

function SlideTransition(props) {
  return <Slide direction="up" {...props} />
}

function FadeTransition(props) {
  return <Fade in="true" {...props} />
}

let script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
let metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
let manifest = "{\"pluginId\":\"Mandelbrot\"}"

let DesignsPage = class DesignsPage extends React.Component {
    state = {
        manifest: manifest,
        script: script,
        metadata: metadata
    }

    handleCreate = () => {
        console.log("create")

        let component = this

        let config = {
            timeout: 10000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true
        }

        let manifest = "{\"pluginId\":\"Mandelbrot\"}"
        let design = { "manifest": manifest, "script": this.state.script, "metadata": this.state.metadata }

        component.props.handleHideCreateDialog()

        axios.post(component.props.config.designs_command_url, design, config)
            .then(function (content) {
                if (content.status == 202) {
                    var designs = component.props.designs.slice()
                    designs.push({uuid:content.data.uuid, selected: false})
                    component.props.handleLoadDesigns(designs, component.props.timestamp)
                }
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleDelete = () => {
        console.log("delete")

        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        let promises = this.props.selected
            .map((uuid) => {
                return axios.delete(component.props.config.designs_command_url + '/' + uuid, config)
            })

        component.props.handleHideConfirmDelete()

        axios.all(promises)
            .then(function (responses) {
                let deletedUuids = responses
                    .filter((res) => {
                        return res.status == 202
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                let designs = component.props.designs
                    .filter((design) => {
                        return !deletedUuids.includes(design.uuid)
                    })
                    .map((design) => {
                        return { uuid: design.uuid, selected: design.selected }
                    })

                component.props.handleChangeSelected([])
                component.props.handleLoadDesigns(designs, component.props.timestamp)
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleModify = () => {
        if (this.props.selected[0]) {
            window.location = this.props.config.web_url + "/admin/designs/" + this.props.selected[0]
        }
    }

    handleScriptChanged = (value) => {
        this.setState({script: value})
    }

    handleMetadataChanged = (value) => {
        this.setState({metadata: value})
    }

    render() {
        return (
            <React.Fragment>
                <CssBaseline />
                <Grid container justify="space-between" alignItems="center">
                    <Grid item xs={12}>
                        <Header landing={'/admin/designs'} title={"Designs"}/>
                    </Grid>
                    <Grid item xs={12}>
                        <DesignsTable/>
                    </Grid>
                    <Grid item xs={12}>
                        <Footer/>
                    </Grid>
                </Grid>
                {this.props.account.role == 'admin' && (
                    <div className={this.props.classes.fabcontainer}>
                        <Button variant="fab" className={this.props.classes.fab} color="primary" onClick={this.props.handleShowCreateDialog}>
                            <AddIcon />
                        </Button>
                        <Button variant="fab" className={this.props.classes.fab} color="secondary" onClick={this.handleModify} disabled={this.props.selected.length != 1}>
                            <EditIcon />
                        </Button>
                        <Button variant="fab" className={this.props.classes.fab} color="inherit" onClick={this.props.handleShowConfirmDelete} disabled={this.props.selected.length == 0}>
                            <DeleteIcon />
                        </Button>
                    </div>
                )}
                {this.props.account.role == 'admin' && (
                    <Dialog className={this.props.classes.dialog} open={this.props.show_create_design} onClose={this.props.handleHideCreateDialog} scroll={"paper"} TransitionComponent={SlideTransition}>
                        <DialogTitle>Create New Design</DialogTitle>
                        <DialogContent>
                            <DesignForm script={this.state.script} metadata={this.state.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
                        </DialogContent>
                        <DialogActions>
                            <Button variant="outlined" color="primary" onClick={this.props.handleHideCreateDialog} color="primary">
                              Cancel
                            </Button>
                            <Button variant="outlined" color="primary" onClick={this.handleCreate} color="primary" autoFocus>
                              Create
                            </Button>
                        </DialogActions>
                    </Dialog>
                )}
                {this.props.account.role == 'admin' && (
                    <Dialog className={this.props.classes.dialog} open={this.props.show_delete_designs} onClose={this.props.handleHideConfirmDelete} scroll={"paper"} TransitionComponent={FadeTransition}>
                        <DialogTitle>Delete Designs</DialogTitle>
                        <DialogContent>
                            {this.props.selected.length == 1 ? (<p>Do you want to delete {this.props.selected.length} item?</p>) : (<p>Do you want to delete {this.props.selected.length} items?</p>)}
                        </DialogContent>
                        <DialogActions>
                            <Button variant="outlined" color="primary" onClick={this.props.handleHideConfirmDelete} color="primary">
                              Cancel
                            </Button>
                            <Button variant="outlined" color="primary" onClick={this.handleDelete} color="primary" autoFocus>
                              Delete
                            </Button>
                        </DialogActions>
                    </Dialog>
                )}
            </React.Fragment>
        )
    }
}

const styles = theme => ({
  fabcontainer: {
    marginLeft: '-25%',
    width: '50%',
    left: '50%',
    position: 'fixed',
    zIndex: 1000,
    bottom: theme.spacing.unit * 2,
    textAlign: 'center'
  },
  fab: {
    margin: theme.spacing.unit
  }
})

DesignsPage.propTypes = {
    config: PropTypes.object.isRequired,
    account: PropTypes.object.isRequired,
    selected: PropTypes.array.isRequired,
    designs: PropTypes.array.isRequired,
    timestamp: PropTypes.number.isRequired,
    show_create_design: PropTypes.bool.isRequired,
    show_delete_designs: PropTypes.bool.isRequired,
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
    config: getConfig(state),
    account: getAccount(state),
    designs: getDesigns(state),
    selected: getSelected(state),
    timestamp: getTimestamp(state),
    show_create_design: getShowCreateDesign(state),
    show_delete_designs: getShowDeleteDesigns(state)
})

const mapDispatchToProps = dispatch => ({
    handleChangeSelected: (selected) => {
        dispatch(setDesignsSelection(selected))
    },
    handleShowConfirmDelete: () => {
        dispatch(showDeleteDesigns())
    },
    handleHideConfirmDelete: () => {
        dispatch(hideDeleteDesigns())
    },
    handleShowCreateDialog: () => {
        dispatch(showCreateDesign())
    },
    handleHideCreateDialog: () => {
        dispatch(hideCreateDesign())
    },
    handleLoadDesigns: (designs, timestamp) => {
        dispatch(loadDesignsSuccess(designs, timestamp))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(DesignsPage))
