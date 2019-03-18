import { combineReducers } from 'redux'

import config from './config'
import account from './account'
import designs from './designs'
import preview from './preview'

export default combineReducers({
    config,
    account,
    designs,
    preview
})
