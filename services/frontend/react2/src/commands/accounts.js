import Cookies from 'universal-cookie'
import axios from 'axios'

const Accounts = class {
    constructor(appConfig, abortController) {
        this.appConfig = appConfig
        this.abortController = abortController
    }

    onLoadAccount = () => {}
    onLoadAccountSuccess = (account) => {}
    onLoadAccountFailure = (error) => {}

    loadAccount() {
        const self = this

        const cookies = new Cookies()

        const axiosConfig = {
            timeout: 5000,
            withCredentials: true,
            signal: self.abortController.signal
        }

        self.onLoadAccount()

        console.log("Loading account...")

        axios.get(this.appConfig.api_url + '/v1/accounts/me', axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                    console.log("Account loaded")
                    let { role, name } = response.data
                    self.onLoadAccountSuccess({ role, name })
                } else if (response.status == 403) {
                    console.log("Not authenticated")
                    cookies.remove('token', {domain: window.location.hostname})
                    self.onLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
                } else {
                    console.log("Can't load account: status = " + response.status)
                    cookies.remove('token', {domain: window.location.hostname})
                    self.onLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
                }
            })
            .catch(function (error) {
                console.log("Can't load account: " + error)
                cookies.remove('token', {domain: window.location.hostname})
                self.onLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
            })
    }
}

export default Accounts
