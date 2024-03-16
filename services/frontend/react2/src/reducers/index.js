import { combineReducers } from 'redux'

import config from './config'
import account from './account'
import designs from './designs'
import design from './design'

export default combineReducers({
    config,
    account,
    designs,
    design
})
