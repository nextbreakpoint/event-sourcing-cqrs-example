import React from 'react'
import PropTypes from 'prop-types'

import { withStyles } from '@material-ui/core/styles'

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
import Dialog from '@material-ui/core/Dialog'
import DialogActions from '@material-ui/core/DialogActions'
import DialogContent from '@material-ui/core/DialogContent'
import DialogTitle from '@material-ui/core/DialogTitle'
import Slide from '@material-ui/core/Slide'

import AddIcon from '@material-ui/icons/Add'
import EditIcon from '@material-ui/icons/Edit'
import DeleteIcon from '@material-ui/icons/Delete'
import CloseIcon from '@material-ui/icons/Close'

import DesignsTable from './DesignsTable'
import DesignItem from './DesignItem'
import NewDesign from './NewDesign'

import { connect } from 'react-redux'

import { showCreateDesign, hideCreateDesign, setDesigns } from './actions/designs'

import axios from 'axios'

const base_url = 'https://localhost:8080'

function Transition(props) {
  return <Slide direction="up" {...props} />
}

let script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
let metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
let manifest = "{\"pluginId\":\"Mandelbrot\"}"

let Designs = class Designs extends React.Component {
    state = {
        manifest: manifest,
        script: script,
        metadata: metadata
    }

    componentDidMount = () => {
        let timestamp = Date.now();

        let component = this

        try {
            if (typeof(EventSource) !== "undefined") {
                var source = new EventSource(component.props.config.web_url + "/watch/designs/" + timestamp, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                  component.loadDesigns(timestamp)
                }

                source.addEventListener("update",  function(event) {
                   let timestamp = Number(event.lastEventId)

                   console.log(event)

                   if (component.state.timestamp == undefined || timestamp > component.state.timestamp) {
                      console.log("Reload designs")

                      component.loadDesigns(timestamp)
                   }
                }, false)
            } else {
                console.log("EventSource not available")
            }
        } catch (e) {
           console.log(e)
        }
    }

    loadDesigns = (timestamp) => {
        let component = this

        let config = {
            timeout: 10000,
            withCredentials: true
        }

        axios.get(component.props.config.designs_query_url, config)
            .then(function (content) {
                let designs = content.data.map((design) => { return { uuid: design.uuid, checksum: design.checksum, selected: false }})

                component.props.handleDesignsLoaded(designs, timestamp)
            })
            .catch(function (error) {
                console.log(error)

                component.props.handleDesignsLoaded([], timestamp)
            })
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

        component.props.handleHideEditor()

        axios.post(component.props.config.designs_command_url, design, config)
            .then(function (content) {
                if (content.status == 201) {
                    var designs = component.state.designs.slice()

                    designs.push({uuid:content.data.uuid, selected: false})

                    component.props.handleDesignsLoaded(designs, this.props.timestamp)
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

        let promises = this.state.designs
            .filter((design) => {
                return design.selected
            }).map((design) => {
                return axios.delete(component.props.config.designs_command_url + '/' + design.uuid, config)
            })

        axios.all(promises)
            .then(function (responses) {
                let deletedUuids = responses
                    .filter((res) => {
                        return res.status == 200
                    })
                    .map((res) => {
                        return res.config.url.substring(res.config.url.lastIndexOf("/") + 1)
                    })

                let designs = component.state.designs
                    .filter((design) => {
                        return !deletedUuids.includes(design.uuid)
                    })
                    .map((design) => {
                        return { uuid: design.uuid, selected: design.selected }
                    })

                component.props.handleDesignsLoaded(designs, this.props.timestamp)
            })
            .catch(function (error) {
                console.log(error)
            })
    }

    handleModify = () => {
        console.log("modify")
    }

    handleScriptChanged = (value) => {
        //this.setState(Object.assign(this.state, {script: value}))
    }

    handleMetadataChanged = (value) => {
        //this.setState(Object.assign(this.state, {metadata: value}))
    }

    render() {
        return (
            <div>
                <DesignsTable/>
                {this.props.role == 'admin' && <div className={this.props.classes.fabcontainer}>
                    <Button variant="fab" className={this.props.classes.fab} color="primary" onClick={this.props.handleShowEditor}>
                        <AddIcon />
                    </Button>
                    <Button variant="fab" className={this.props.classes.fab} color="secondary" onClick={this.handleModify} disabled={true}>
                        <EditIcon />
                    </Button>
                    <Button variant="fab" className={this.props.classes.fab} color="inherit" onClick={this.handleDelete} disabled={true}>
                        <DeleteIcon />
                    </Button>
                </div>}
                {this.props.role == 'admin' && <Dialog fullScreen={false} className={this.props.classes.dialog} open={this.props.open} onClose={this.props.handleHideEditor} scroll={"paper"} TransitionComponent={Transition}>
                    <DialogTitle>Create New Design</DialogTitle>
                    <DialogContent>
                        <NewDesign script={this.state.script} metadata={this.state.metadata} onScriptChanged={this.handleScriptChanged} onMetadataChanged={this.handleMetadataChanged}/>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={this.props.handleHideEditor} color="primary">
                          Cancel
                        </Button>
                        <Button onClick={this.handleCreate} color="primary">
                          Create
                        </Button>
                    </DialogActions>
                </Dialog>}
            </div>
        )
    }
}

Designs.propTypes = {
  config: PropTypes.object.isRequired,
  designs: PropTypes.array.isRequired,
  timestamp: PropTypes.number.isRequired,
  role: PropTypes.string.isRequired,
  open: PropTypes.bool.isRequired,
  classes: PropTypes.object.isRequired,
  theme: PropTypes.object.isRequired
}

const styles = theme => ({
  fabcontainer: {
    'position': 'fixed',
    'z-index': 1000,
    'bottom': theme.spacing.unit * 2,
    'right': theme.spacing.unit * 2
  },
  fab: {
    'margin': theme.spacing.unit
  },
  dialog: {
  }
})

const mapStateToProps = state => {
    console.log(JSON.stringify(state))

    return {
        config: state.designs.config,
        role: state.designs.account.role,
        designs: state.designs.designs,
        timestamp: state.designs.timestamp,
        open: state.designs.show_create_design
    }
}

const mapDispatchToProps = dispatch => ({
    handleShowEditor: () => {
        dispatch(showCreateDesign())
    },
    handleHideEditor: () => {
        dispatch(hideCreateDesign())
    },
    handleDesignsLoaded: (designs, timestamp) => {
        dispatch(setDesigns(designs, timestamp))
    }
})

export default withStyles(styles, { withTheme: true })(connect(mapStateToProps, mapDispatchToProps)(Designs))
