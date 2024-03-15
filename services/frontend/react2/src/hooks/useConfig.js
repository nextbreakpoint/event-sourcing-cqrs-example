import { useRef, useEffect, useCallback } from 'react'
import { useDispatch } from 'react-redux'

import {
    loadConfig,
    loadConfigSuccess,
    loadConfigFailure
} from '../actions/config'

export default function useConfig() {
    const configRef = useRef(window.config)
    const dispatch = useDispatch()

    const onLoadConfig = useCallback(() => dispatch(loadConfig()), [dispatch])
    const onLoadConfigSuccess = useCallback((config) => dispatch(loadConfigSuccess(config)), [dispatch])
    const onLoadConfigFailure = useCallback((error) => dispatch(loadConfigFailure(error)), [dispatch])

    useEffect(() => {
        console.log("Loading config...")

        onLoadConfig()

        console.log("Config loaded")

        onLoadConfigSuccess(configRef.current)

        return () => {}
    }, [onLoadConfig, onLoadConfigSuccess, onLoadConfigFailure])
}
