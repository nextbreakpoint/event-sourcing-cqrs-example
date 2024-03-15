import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'

import Preview from '../commands/preview'

import {
    getConfig
} from '../actions/config'

export default function usePreview({ design, appConfig, onLoadPreview, onLoadPreviewSuccess, onLoadPreviewFailure }) {
    const abortControllerRef = useRef(null)
    const config = useSelector(getConfig)
    const dispatch = useDispatch()

    useEffect(() => {
        const preview = new Preview(config, abortControllerRef)
        preview.onLoadPreview = onLoadPreview
        preview.onLoadPreviewSuccess = onLoadPreviewSuccess
        preview.onLoadPreviewFailure = onLoadPreviewFailure

        const timeout = setTimeout(() => {
            abortControllerRef.current = new AbortController()
            preview.loadPreview(design)
        }, 2000)

        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort()
            }
            if (timeout) {
                clearTimeout(timeout)
            }
        }
    }, [design, appConfig, onLoadPreview, onLoadPreviewSuccess, onLoadPreviewFailure])
}

