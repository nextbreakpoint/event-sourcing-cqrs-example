import { useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'

import {
    getConfig
} from '../actions/config'

export default function useDesign({ uuid, doLoadDesign }) {
    const config = useSelector(getConfig)
    const dispatch = useDispatch()

    useEffect(() => {
        try {
            if (typeof(EventSource) !== "undefined") {
                const revision = "0000000000000000-0000000000000000"

                const source = new EventSource(config.api_url + "/v1/designs/watch?designId=" + uuid + "&revision=" + revision, { withCredentials: true })

                source.onerror = function(error) {
                   console.log(error)
                }

                source.onopen = function() {
                    doLoadDesign(revision)
                }

                source.addEventListener("update",  function(event) {
//                    console.log(event.data)
                    doLoadDesign(revision)
                }, false)

                return () => {
                    source.close()
                }
            } else {
                console.log("Can't watch resource. EventSource not supported by the browser")
                doLoadDesign(revision)
            }
        } catch (e) {
           console.log("Can't watch resource: " + e)
           doLoadDesign(revision)
        }

        return () => {}
    }, [config, doLoadDesign])
}

