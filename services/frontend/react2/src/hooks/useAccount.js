import { useRef, useEffect, useCallback } from 'react'
import { useSelector, useDispatch } from 'react-redux'

import LoadAccount from '../commands/loadAccount'

import {
    getConfig
} from '../actions/config'

import {
    loadAccount,
    loadAccountSuccess,
    loadAccountFailure
} from '../actions/account'

export default function useAccount() {
    const abortControllerRef = useRef(null)
    const config = useSelector(getConfig)
    const dispatch = useDispatch()

    const onLoadAccount = useCallback(() => dispatch(loadAccount()), [dispatch])
    const onLoadAccountSuccess = useCallback((account) => dispatch(loadAccountSuccess(account)), [dispatch])
    const onLoadAccountFailure = useCallback((error) => dispatch(loadAccountFailure(error)), [dispatch])

    useEffect(() => {
        abortControllerRef.current = new AbortController()

        const command = new LoadAccount(config, abortControllerRef)
        command.onLoadAccount = onLoadAccount
        command.onLoadAccountSuccess = onLoadAccountSuccess
        command.onLoadAccountFailure = onLoadAccountFailure
        command.run()

        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort()
            }
        }
    }, [config, onLoadAccount, onLoadAccountSuccess, onLoadAccountFailure])
}

