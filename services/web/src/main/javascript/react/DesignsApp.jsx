import React from 'react'
import ReactDOM from 'react-dom'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './components/shared/Config'
import Account from './components/shared/Account'
import Designs from './components/designs/Designs'
import DesignsPage from './components/designs/DesignsPage'

const store = createStore(reducers)

ReactDOM.render(
    <Provider store={store}>
        <Config>
            <Account>
                <Designs>
                    <DesignsPage/>
                </Designs>
            </Account>
        </Config>
    </Provider>,
    document.querySelector('#app')
)
