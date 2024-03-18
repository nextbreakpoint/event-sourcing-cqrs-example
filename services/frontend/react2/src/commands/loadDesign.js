import axios from 'axios'
import computePercentage from '../modules/percentage'

const LoadDesign = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onLoadDesign = () => {}
    onLoadDesignSuccess = (design, revision) => {}
    onLoadDesignFailure = (error) => {}

    run(revision, uuid) {
        const self = this

        const date = new Date()

        const axiosConfig = {
            timeout: 30000,
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Loading design...")

        self.onLoadDesign()

        axios.get(self.appConfig.api_url + '/v1/designs/' + uuid + '?draft=true', axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Design loaded")
                    const design = response.data
                    const data = JSON.parse(design.json)
                    design.manifest = data.manifest
                    design.metadata = data.metadata
                    design.script = data.script
                    design.draft = design.levels != 8
                    design.published = design.published
                    design.percentage = computePercentage(design, [0,1,2,3,4,5,6,7])
                    design.preview_percentage = computePercentage(design, [0,1,2])
//                    console.log(design)
                    self.onLoadDesignSuccess(design, design.revision)
                } else {
                    console.log("Can't load design: status = " + response.status)
                    self.onLoadDesignFailure("Can't load design")
                }
            })
            .catch(function (error) {
                console.log("Can't load design: " + error)
                self.onLoadDesignFailure("Can't load design")
            })
    }
}

export default LoadDesign
