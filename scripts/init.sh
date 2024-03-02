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
      "created": {
          "type": "date",
          "format": "date_time"
      },
      "updated": {
        "type": "date",
        "format": "date_time"
      }
    }
  }
}
EOF

makeIndex() {
    RESULT=$(curl -s --head "http://localhost:9200/$1" | head -n 1 | cut -d$' ' -f2)
    if [ "$RESULT" = "200" ]
    then
        echo "Index $1 already exists"
    else
        echo "Creating index $1..."
        curl -s -X PUT http://localhost:9200/$1 -H 'content-type: application/json' -d @/tmp/data.json
        [ $? = 0 ]
    fi
}

until makeIndex designs; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done
until makeIndex designs_draft; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done

until makeIndex test_designs_query; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done
until makeIndex test_designs_query_draft; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done

