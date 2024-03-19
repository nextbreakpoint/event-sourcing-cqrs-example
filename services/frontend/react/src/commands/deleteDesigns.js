import axios from 'axios'

const DeleteDesigns = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onDeleteDesigns = () => {}
    onDeleteDesignsSuccess = (message) => {}
    onDeleteDesignsFailure = (error) => {}

    run(selection) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Deleting designs...")

        self.onDeleteDesigns()

        const promises = selection
           .map((uuid) => {
                return axios.delete(self.appConfig.api_url + '/v1/designs/' + uuid + '?draft=true', axiosConfig)
            })

        axios.all(promises)
            .then(function (responses) {
                const deletedUuids = responses
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
                    self.onDeleteDesignsSuccess("Your request has been received. The designs will be deleted shortly")
                } else {
                    self.onDeleteDesignsFailure("Can't delete the designs")
                }
            })
            .catch(function (error) {
                console.log("Can't delete the designs: " + error)
                self.onDeleteDesignsFailure("Can't delete the designs")
            })
    }
}

export default DeleteDesigns
