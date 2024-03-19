import axios from 'axios'

const CreateDesign = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onCreateDesign = () => {}
    onCreateDesignSuccess = (message) => {}
    onCreateDesignFailure = (error) => {}

    run(design) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Creating design...")

        self.onCreateDesign()

        axios.post(self.appConfig.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        axios.post(self.appConfig.api_url + '/v1/designs', design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 202 || response.status == 200) {
                                    console.log("Design created")
                                    self.onCreateDesignSuccess("Your request has been received. The design will be created shortly")
                                } else {
                                    console.log("Can't create the design: status = " + response.status)
                                    self.onCreateDesignFailure("Can't create the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't create the design: " + error)
                                self.onCreateDesignFailure("Can't create the design")
                            })
                     } else {
                        console.log("Can't create the design: statue " + result.status)
                        self.onCreateDesignFailure("Can't create the design")
                     }
                } else {
                    console.log("Can't create the design: status = " + response.status)
                    self.onCreateDesignFailure("Can't create the design")
                }
            })
            .catch(function (error) {
                console.log("Can't create the design: " + error)
                self.onCreateDesignFailure("Can't create the design")
            })
    }
}

export default CreateDesign
