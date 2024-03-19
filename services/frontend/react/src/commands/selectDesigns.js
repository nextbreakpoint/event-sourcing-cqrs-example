import axios from 'axios'

const SelectDesigns = class {
    constructor(appConfig, abortControllerRef) {
        this.appConfig = appConfig
        this.abortControllerRef = abortControllerRef
    }

    onSelectDesigns = () => {}
    onSelectDesignsSuccess = (message) => {}
    onSelectDesignsFailure = (error) => {}

    run(selection) {
        const self = this

        const axiosConfig = {
            timeout: 30000,
            withCredentials: true,
            signal: self.abortControllerRef.current.signal
        }

        console.log("Selecting designs...")

        self.onSelectDesigns()

        const promises = selection
            .map((uuid) => {
                return axios.get(config.api_url + '/v1/designs/' + uuid + "?draft=true", axiosConfig)
            })

        axios.all(promises)
            .then(function (responses) {
                const documents = responses
                    .filter((res) => {
                        return res.status == 200
                    })
                    .map((res) => {
                        return {
                            uuid: res.data.uuid,
                            design: JSON.parse(res.data.json)
                        }
                    })
                console.log("Designs selected")
                self.onSelectDesignsSuccess(documents);
            })
            .catch(function (error) {
                console.log("Can't select the designs: " + error)
                self.onSelectDesignsFailure("Can't select the designs")
            })
    }
}

export default SelectDesigns
