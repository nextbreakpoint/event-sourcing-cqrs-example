import React from 'react'
import { createRoot } from 'react-dom/client';

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './components/shared/Config'
import Account from './components/shared/Account'
import Design from './components/design/Design'
import DesignPage from './components/design/DesignPage'

import { ThemeProvider, createTheme } from '@mui/material/styles';

const theme = createTheme();

const store = createStore(reducers)

const root = createRoot(document.getElementById('app'));

const uuid = document.querySelector('#uuid').value

root.render(
    <Provider store={store}>
        <Config>
            <ThemeProvider theme={theme}>
                <Account>
                    <Design uuid={uuid}>
                        <DesignPage uuid={uuid}/>
                    </Design>
                </Account>
           </ThemeProvider>
        </Config>
    </Provider>
)
