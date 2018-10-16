import React from 'react'
import ReactDOM from 'react-dom'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './Config'
import Account from './Account'
import PreviewPage from './PreviewPage'

const store = createStore(reducers)

ReactDOM.render(<Provider store={store}><Config><Account><PreviewPage /></Account></Config></Provider>, document.querySelector('#app'))
