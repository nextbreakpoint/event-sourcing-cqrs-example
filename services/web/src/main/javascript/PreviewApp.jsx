import React from 'react'
import ReactDOM from 'react-dom'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './Config'
import Account from './Account'
import Preview from './Preview'
import PreviewPage from './PreviewPage'

const store = createStore(reducers)

var uuid = "00000000-0000-0000-0000-000000000000"

const regexp = /https?:\/\/.*\/admin\/designs\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/g
const match = regexp.exec(window.location.href)

if (match != null && match.length == 2) {
    uuid = match[1]
}

ReactDOM.render(<Provider store={store}><Config><Account><Preview uuid={uuid}><PreviewPage uuid={uuid}/></Preview></Account></Config></Provider>, document.querySelector('#app'))
