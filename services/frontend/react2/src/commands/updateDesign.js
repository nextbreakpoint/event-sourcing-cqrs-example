import axios from 'axios'

const UpdateDesign = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onUpdateDesign = () => {}
    onUpdateDesignSuccess = (message) => {}
    onUpdateDesignFailure = (error) => {}

    run(uuid, design) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Updating design...")

        self.onUpdateDesign()

        axios.post(self.appConfig.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        axios.put(self.appConfig.api_url + '/v1/designs/' + uuid, design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    console.log("Design updated")
                                    self.onUpdateDesignSuccess("Your request has been received. The design will be updated shortly")
                                } else {
                                    console.log("Can't update the design: status = " + response.status)
                                    self.onUpdateDesignFailure("Can't update the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't update the design: " + error)
                                self.onUpdateDesignFailure("Can't update the design")
                            })
                     } else {
                        console.log("Can't update the design: statue " + result.status)
                        self.onUpdateDesignFailure("Can't update the design")
                     }
                } else {
                    console.log("Can't update the design: status = " + response.status)
                    self.onUpdateDesignFailure("Can't update the design")
                }
            })
            .catch(function (error) {
                console.log("Can't update the design: " + error)
                self.onUpdateDesignFailure("Can't update the design")
            })
    }
}

export default UpdateDesign
