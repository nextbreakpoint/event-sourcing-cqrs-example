import { useRef, useCallback, useEffect } from 'react'

export default function useConfig({ onLoadConfig, onLoadConfigSuccess, onLoadConfigFailure }) {
  const onLoadConfigCallback = useCallback(() => onLoadConfig())
  const onLoadConfigSuccessCallback = useCallback((account) => onLoadConfigSuccess(account))
  const onLoadConfigFailureCallback = useCallback((error) => onLoadConfigFailure(error))

  const configRef = useRef(window.config)

  useEffect(() => {
    console.log("Loading config...")

    onLoadConfigCallback()

    console.log("Config loaded")

    onLoadConfigSuccessCallback(configRef.current)

    return () => {}
  }, [])
}
