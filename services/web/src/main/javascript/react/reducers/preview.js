import { combineReducers } from 'redux'

import content from './preview/content'
import dialog from './preview/dialog'

export default combineReducers({
    content,
    dialog
})
