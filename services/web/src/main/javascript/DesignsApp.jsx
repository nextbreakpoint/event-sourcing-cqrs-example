import React from 'react'
import ReactDOM from 'react-dom'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './Config'
import Account from './Account'
import DesignsPage from './DesignsPage'

const store = createStore(reducers)

ReactDOM.render(<Provider store={store}><Config><Account><DesignsPage /></Account></Config></Provider>, document.querySelector('#app'))
