import axios from 'axios'

const UpdateDesigns = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onUpdateDesigns = () => {}
    onUpdateDesignsSuccess = (message) => {}
    onUpdateDesignsFailure = (error) => {}

    run(documents, callback) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Updating designs...")

        self.onUpdateDesigns()

        const promises = documents
            .filter((document) => {
                return document !== undefined
            })
            .map((document) => {
                callback(document.design)
                return axios.put(self.appConfig.api_url + '/v1/designs/' + document.uuid, document.design, axiosConfig)
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

                if (failedUuids.length == 0) {
                    self.onUpdateDesignsSuccess("Your request has been received. The designs will be updated shortly")
                } else {
                    console.log("Failed to update designs: " + JSON.stringify(failedUuids))
                    self.onUpdateDesignsFailure("Can't update the designs")
                }
            })
            .catch(function (error) {
                console.log("Can't update the designs: " + error)
                self.onUpdateDesignsFailure("Can't update the designs")
            })
    }
}

export default UpdateDesigns
