import { useRef, useEffect } from 'react'

import Cookies from 'universal-cookie'
import axios from 'axios'

export default function useAccount({ appConfig, onLoadAccount, onLoadAccountSuccess, onLoadAccountFailure }) {
  const cookiesRef = useRef(new Cookies())
  const abortControllerRef = useRef(new AbortController())

  useEffect(() => {
    console.log("Loading account...")

    onLoadAccount()

    const cookies = cookiesRef.current
    const controller = abortControllerRef.current

    const axiosConfig = {
        timeout: 5000,
        withCredentials: true,
        signal: controller.signal
    }

    axios.get(appConfig.api_url + '/v1/accounts/me', axiosConfig)
        .then(function (response) {
            if (response.status == 200) {
                console.log("Account loaded")
                let { role, name } = response.data
                onLoadAccountSuccess({ role, name })
            } else if (response.status == 403) {
                console.log("Not authenticated")
                cookies.remove('token', {domain: window.location.hostname})
                onLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
            } else {
                console.log("Can't load account: status = " + response.status)
                cookies.remove('token', {domain: window.location.hostname})
                onLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
            }
        })
        .catch(function (error) {
            console.log("Can't load account: " + error)
            cookies.remove('token', {domain: window.location.hostname})
            onLoadAccountSuccess({ "role": "anonymous", "name": "Stranger" })
        })
    return () => { controller.abort() }
  }, [appConfig, onLoadAccount, onLoadAccountSuccess, onLoadAccountFailure])
}

