import React from 'react'
import { useState, useCallback } from 'react';

import { Editor, EditorState, ContentState, CompositeDecorator } from 'draft-js'

const KEYWORD_REGEXP = /translation|rotation|scale|point|julia|options/g

function findWithRegex(regex, contentBlock, callback) {
  const text = contentBlock.getText();
  let matchArr, start;
  while ((matchArr = regex.exec(text)) !== null) {
    start = matchArr.index;
    callback(start, start + matchArr[0].length);
  }
}

function keywordStrategy(contentBlock, callback, contentState) {
  findWithRegex(KEYWORD_REGEXP, contentBlock, callback);
}

const KeywordSpan = (props) => {
  return <span class="keyword">{props.children}</span>
}

const compositeDecorator = new CompositeDecorator([
  {
    strategy: keywordStrategy,
    component: KeywordSpan
  }
])

export default function MetadataEditor({ initialValue, readOnly, onContentChanged }) {
    const [ editorState, setEditorState ] = useState(() => EditorState.createWithContent(ContentState.createFromText(initialValue), compositeDecorator))

    const onChangeCallback = useCallback((editorState) => {
        setEditorState(editorState)
        if (onContentChanged) {
            onContentChanged(editorState.getCurrentContent().getPlainText())
        }
    }, [onContentChanged])

    return (
      <div class="editor">
        <Editor readOnly={readOnly} editorState={editorState} onChange={onChangeCallback}/>
      </div>
    )
}
