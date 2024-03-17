import axios from 'axios'

const DownloadDesign = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onDownloadDesign = () => {}
    onDownloadDesignSuccess = (message) => {}
    onDownloadDesignFailure = (error) => {}

    run(uuid) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            metadata: {'content-type': 'application/json'},
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Preparing design for download...")

        self.onDownloadDesign()

        axios.get(config.api_url + '/v1/designs/' + uuid + '?draft=true', axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     const result = response.data
                     if (result.status == "ACCEPTED") {
                        const design = JSON.parse(response.data.json)
                        const axiosConfigDownload = {
                            timeout: 30000,
                            metadata: {'content-type': 'application/json'},
                            withCredentials: true,
                            responseType: "blob",
                            signal: self.abortControllerRef.current.signal
                        }
                        axios.post(self.appConfig.api_url + '/v1/designs/download', design, axiosConfigDownload)
                            .then(function (response) {
                                if (response.status == 200) {
                                    const url = window.URL.createObjectURL(response.data);
                                    const a = document.createElement('a');
                                    a.href = url;
                                    a.download = uuid + '.zip';
                                    a.click();
                                    console.log("Design is ready for download")
                                    self.onDownloadDesignSuccess("The design will be downloaded shortly")
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
