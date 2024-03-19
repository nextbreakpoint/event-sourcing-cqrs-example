import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'

import RenderPreview from '../commands/renderPreview'

import {
    getConfig
} from '../actions/config'

export default function usePreview({ design, onLoadPreview, onLoadPreviewSuccess, onLoadPreviewFailure }) {
    const abortControllerRef = useRef(null)
    const config = useSelector(getConfig)
    const dispatch = useDispatch()

    useEffect(() => {
        const timeout = setTimeout(() => {
            abortControllerRef.current = new AbortController()
            const command = new RenderPreview(config, abortControllerRef)
            command.onLoadPreview = onLoadPreview
            command.onLoadPreviewSuccess = onLoadPreviewSuccess
            command.onLoadPreviewFailure = onLoadPreviewFailure
            command.run(design)
        }, 2000)

        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort()
            }
            if (timeout) {
                clearTimeout(timeout)
            }
        }
    }, [design, onLoadPreview, onLoadPreviewSuccess, onLoadPreviewFailure])
}

