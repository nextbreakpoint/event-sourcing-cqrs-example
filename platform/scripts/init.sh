#!/bin/sh

set -e

cat <<EOF >/tmp/data.json
{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    }
  },
  "mappings": {
    "properties": {
      "userId": {
        "type": "keyword"
      },
      "commandId": {
        "type": "keyword"
      },
      "designId": {
        "type": "keyword"
      },
      "data": {
        "type": "text"
      },
      "checksum": {
        "type": "keyword"
      },
      "status": {
        "type": "keyword"
      },
      "modified": {
        "type": "date",
        "format": "date_time"
      }
    }
  }
}
EOF

function makeIndex {
  RESULT=$(curl -s --head http://localhost:9200/$1 | head -n 1 | cut -d$' ' -f2)
  if [ $RESULT -eq 404 ]
  then
    echo "Creating index $1..."
    until curl -s -X PUT http://localhost:9200/designs -H 'content-type: application/json' -d @/tmp/data.json; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done &
    echo "Index $1 created"
  else
    echo "Index $1 already exists"
  fi
}

makeIndex designs
makeIndex designs_draft
makeIndex test_designs_query
makeIndex test_designs_query_draft

