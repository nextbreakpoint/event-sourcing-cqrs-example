import { useRef, useEffect } from 'react'

export default function useConfig({ onLoadConfig, onLoadConfigSuccess, onLoadConfigFailure }) {
  const configRef = useRef(window.config)

  useEffect(() => {
    console.log("Loading config...")

    onLoadConfig()

    console.log("Config loaded")

    onLoadConfigSuccess(configRef.current)

    return () => {}
  }, [onLoadConfig, onLoadConfigSuccess, onLoadConfigFailure])
}
