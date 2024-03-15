import axios from 'axios'

const Preview = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onLoadPreview = () => {}
    onLoadPreviewSuccess = (checksum) => {}
    onLoadPreviewFailure = (error) => {}

    loadPreview(design) {
        const self = this

        const date = new Date()

        const axiosConfig = {
            timeout: 5000,
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        self.onLoadPreview("Rendering...")

        console.log("Validating design...")

        axios.post(self.appConfig.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     console.log("Design validated")
                     const result = response.data
                     console.log("Status: " + result.status)
                     if (result.status == "ACCEPTED") {
                        console.log("Rendering design...")
                        axios.post(self.appConfig.api_url + '/v1/designs/render', design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 200) {
                                    console.log("Design rendered")
                                    const imageUrl = self.appConfig.api_url + '/v1/designs/image/' + response.data.checksum + '?t=' + date.getTime()
                                    self.onLoadPreviewSuccess("Last updated " + date.toISOString(), imageUrl)
                                } else {
                                    console.log("Can't render the design: status = " + response.status)
                                    self.onLoadPreviewFailure("Can't render the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't render the design: " + error)
                                self.onLoadPreviewFailure("Can't render the design")
                            })
                     } else {
                        const errors = result.errors.join(', ');
                        console.log("The design contains some errors: " + errors)
                        self.onLoadPreviewFailure("The design contains some errors: " + errors)
                     }
                } else {
                    console.log("Can't validate the design: status = " + response.status)
                    self.onLoadPreviewFailure("Can't validate the design")
                }
            })
            .catch(function (error) {
                console.log("Can't validate the design: " + error)
                self.onLoadPreviewFailure("Can't validate the design")
            })
    }
}

export default Preview
