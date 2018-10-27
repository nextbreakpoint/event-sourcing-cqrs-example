import { combineReducers } from 'redux'

import content from './designs/content'
import dialog from './designs/dialog'
import sorting from './designs/sorting'
import selection from './designs/selection'
import pagination from './designs/pagination'

export default combineReducers({
    content,
    sorting,
    selection,
    pagination,
    dialog
})
