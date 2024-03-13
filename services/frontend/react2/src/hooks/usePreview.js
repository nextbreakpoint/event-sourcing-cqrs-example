import { useRef, useEffect } from 'react'

import axios from 'axios'

export default function usePreview({ design, appConfig, onLoadPreview, onLoadPreviewSuccess, onLoadPreviewFailure }) {
  const abortControllerRef = useRef(null)

  useEffect(() => {
    const handlePreview = (design) => {
        const date = new Date()

        const axiosConfig = {
            timeout: 5000,
            withCredentials: true,
            signal: abortControllerRef.current.signal
        }

        onLoadPreview("Rendering...")

        console.log("Validating design...")

        axios.post(appConfig.api_url + '/v1/designs/validate', design, axiosConfig)
            .then(function (response) {
                if (response.status == 200) {
                     console.log("Design validated")
                     const result = response.data
                     console.log("Status: " + result.status)
                     if (result.status == "ACCEPTED") {
                        console.log("Rendering design...")

                        axios.post(appConfig.api_url + '/v1/designs/render', design, axiosConfig)
                            .then(function (response) {
                                if (response.status == 200) {
                                    console.log("Design rendered")
                                    onLoadPreviewSuccess("Last updated " + date.toISOString(), response.data.checksum, date.getTime())
                                } else {
                                    console.log("Can't render the design: status = " + response.status)
                                    onLoadPreviewFailure("Can't render the design")
                                }
                            })
                            .catch(function (error) {
                                console.log("Can't render the design: " + error)
                                onLoadPreviewFailure("Can't render the design")
                            })
                     } else {
                        const errors = result.errors.join(', ');
                        console.log("The design contains some errors: " + errors)
                        onLoadPreviewFailure("The design contains some errors: " + errors)
                     }
                } else {
                    console.log("Can't validate the design: status = " + response.status)
                    onLoadPreviewFailure("Can't validate the design")
                }
            })
            .catch(function (error) {
                console.log("Can't validate the design: " + error)
                onLoadPreviewFailure("Can't validate the design")
            })
    }

    const timeout = setTimeout(() => {
        abortControllerRef.current = new AbortController()
        handlePreview(design)
    }, 2000)

    return () => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort()
        }
        if (timeout) {
            clearTimeout(timeout)
        }
    }
  }, [appConfig, design, onLoadPreview, onLoadPreviewSuccess, onLoadPreviewFailure])
}

