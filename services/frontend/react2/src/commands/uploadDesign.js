import axios from 'axios'

const UploadDesign = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onUploadDesign = () => {}
    onUploadDesignSuccess = (message) => {}
    onUploadDesignFailure = (error) => {}

    run(file) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Uploading design...")

        self.onUploadDesign()

        const formData = new FormData()
        formData.append('file', file)

        axios.post(self.appConfig.api_url + '/v1/designs/upload', formData, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                    if (response.data.errors.length == 0) {
                        const design = {
                            manifest: response.data.manifest,
                            metadata: response.data.metadata,
                            script: response.data.script
                        }
                        console.log("Design uploaded")
                        self.onUploadDesignSuccess(design)
                    } else {
                        console.log("Can't upload the file: errors = " + response.data.errors)
                        self.onUploadDesignFailure("Can't upload the file")
                    }
                } else {
                    console.log("Can't upload the file: status = " + response.status)
                    self.onUploadDesignFailure("Can't upload the file")
                }
            })
            .catch(function (error) {
                console.log("Can't upload the file: " + error)
                self.onUploadDesignFailure("Can't upload the file")
            })
    }
}

export default UploadDesign
