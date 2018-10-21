import * as Types from '../constants/ActionTypes'

import * as account from '../reducers/account'

export const loadAccount = () => ({
  type: Types.ACCOUNT_LOAD
})

export const loadAccountSuccess = (account) => ({
  type: Types.ACCOUNT_LOAD_SUCCESS, account
})

export const loadAccountFailure = (error) => ({
  type: Types.ACCOUNT_LOAD_FAILURE, error
})

export const getAccount = (state) => {
    return account.getAccount(state)
}

export const getAccountStatus = (state) => {
    return account.getAccountStatus(state)
}
