import axios from 'axios'
import computePercentage from '../modules/percentage'

const LoadDesigns = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onLoadDesigns = () => {}
    onLoadDesignsSuccess = (designs, total, revision) => {}
    onLoadDesignsFailure = (error) => {}

    run(revision, pagination) {
        const self = this

        console.log("page " + pagination.page)

        const date = new Date()

        const axiosConfig = {
            timeout: 30000,
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Loading designs...")

        self.onLoadDesigns()

        axios.get(self.appConfig.api_url + '/v1/designs?draft=true&from=' + (pagination.page * pagination.pageSize) + '&size=' + pagination.pageSize, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Designs loaded")
                    const designs = response.data.designs.map((design) => { return {
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
                    }})
                    const total = response.data.total
                    self.onLoadDesignsSuccess(designs, total, revision)
                } else {
                    console.log("Can't load designs: status = " + content.status)
                    self.onLoadDesignsFailure("Can't load designs")
                }
            })
            .catch(function (error) {
                console.log("Can't load designs " + error)
                self.onLoadDesignsFailure("Can't load designs")
            })
    }
}

export default LoadDesigns
