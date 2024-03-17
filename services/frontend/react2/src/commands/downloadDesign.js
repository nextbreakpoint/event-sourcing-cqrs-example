import axios from 'axios'

const DownloadDesign = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onDownloadDesign = () => {}
    onDownloadDesignSuccess = (message) => {}
    onDownloadDesignFailure = (error) => {}

    run(design) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Downloading design...")

        self.onDownloadDesign()

        axios.post(self.appConfig.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        const axiosConfigUpload = {
                            timeout: 30000,
                            metadata: {'content-type': 'application/json'},
                            withCredentials: true,
                            responseType: "blob",
                            signal: self.abortControllerRef.current.signal
                        }
                        axios.post(self.appConfig.api_url + '/v1/designs/download', design, axiosConfigUpload)
                            .then(function (response) {
                                if (response.status == 200) {
                                    const url = window.URL.createObjectURL(response.data);
                                    const a = document.createElement('a');
                                    a.href = url;
                                    a.download = uuid + '.zip';
                                    a.click();
                                    console.log("Design downloaded")
                                    self.onDownloadDesignSuccess("The design has been downloaded")
                                } else {
                                    console.log("Can't download the design: status = " + response.status)
                                    self.onDownloadDesignFailure("Can't download the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't download the design: " + error)
                                self.onDownloadDesignFailure("Can't download the design")
                            })
                     } else {
                        console.log("Can't download the design: status = " + result.status)
                        self.onDownloadDesignFailure("Can't download the design")
                     }
                } else {
                    console.log("Can't download the design: status = " + response.status)
                    self.onDownloadDesignFailure("Can't download the design")
                }
            })
            .catch(function (error) {
                console.log("Can't download the design: " + error)
                self.onDownloadDesignFailure("Can't download the design")
            })
    }
}

export default DownloadDesign
