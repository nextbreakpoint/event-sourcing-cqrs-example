import React from 'react'
import PropTypes from 'prop-types'

import Button from '@material-ui/core/Button'
import Card from '@material-ui/core/Card'
import CardHeader from '@material-ui/core/CardHeader'
import CardContent from '@material-ui/core/CardContent'

let NewDesign = class NewDesign extends React.Component {
    constructor(props) {
        super(props)

        let script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
        let metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
        let manifest = "{\"pluginId\":\"Mandelbrot\"}"

        this.state = {script: script, metadata: metadata, manifest: manifest}

        this.handleCreateDesign = this.handleCreateDesign.bind(this)
        this.handleScriptChanged = this.handleScriptChanged.bind(this)
        this.handleMetadataChanged = this.handleMetadataChanged.bind(this)
    }

    handleCreateDesign(e) {
        e.preventDefault()
        this.props.onCreate(this.state)
    }

    handleScriptChanged(e) {
        e.preventDefault()
        let source = e.target
        this.setState({script: source.value, metadata: this.state.metadata, manifest: this.state.manifest})
    }

    handleMetadataChanged(e) {
        e.preventDefault()
        let source = e.target
        this.setState({script: this.state.script, metadata: source.value, manifest: this.state.manifest})
    }

    render() {
        return (
            <Card>
                <CardHeader title="Create new design"></CardHeader>
                <CardContent>
                    <form>
                        <div>
                            <label htmlFor="script">Script</label>
                            <textarea rows="20" id="script" name="script" value={this.state.script} onChange={(e) => this.handleScriptChanged(e)}></textarea>
                        </div>
                        <div>
                            <label htmlFor="metadata">Metadata</label>
                            <textarea rows="20" id="metadata" name="metadata" value={this.state.metadata} onChange={(e) => this.handleMetadataChanged(e)}></textarea>
                        </div>
                        <Button onClick={(e) => this.handleCreateDesign(e)}>Create</Button>
                    </form>
                </CardContent>
            </Card>
        )
    }
}

NewDesign.propTypes = {
  onCreate: PropTypes.func
}

module.exports = NewDesign
