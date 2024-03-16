import { useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'

import {
    getConfig
} from '../actions/config'

export default function useDesigns({ doLoadDesigns }) {
    const config = useSelector(getConfig)
    const dispatch = useDispatch()

    useEffect(() => {
        try {
            if (typeof(EventSource) !== "undefined") {
                const revision = "0000000000000000-0000000000000000"

                const source = new EventSource(config.api_url + "/v1/designs/watch?revision=" + revision, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                    doLoadDesigns(revision)
                }

                source.addEventListener("update",  function(event) {
                    console.log(event.data)
                    doLoadDesigns(revision)
                }, false)

                return () => {
                    source.close()
                }
            } else {
                console.log("Can't watch resource. EventSource not supported by the browser")
                doLoadDesigns(revision)
            }
        } catch (e) {
           console.log("Can't watch resource: " + e)
           doLoadDesigns(revision)
        }

        return () => {}
    }, [config, doLoadDesigns])
}

