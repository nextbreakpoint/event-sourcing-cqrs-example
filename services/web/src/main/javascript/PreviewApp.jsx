import React from 'react'
import ReactDOM from 'react-dom'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './components/shared/Config'
import Account from './components/shared/Account'
import Preview from './components/preview/Preview'
import PreviewPage from './components/preview/PreviewPage'

const store = createStore(reducers)

const uuid = document.querySelector('#uuid').value

ReactDOM.render(
    <Provider store={store}>
        <Config>
            <Account>
                <Preview uuid={uuid}>
                    <PreviewPage uuid={uuid}/>
                </Preview>
            </Account>
        </Config>
    </Provider>,
    document.querySelector('#app')
)
