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

until curl -X PUT http://localhost:9200/designs -H 'content-type: application/json' -d @/tmp/data.json; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done &

until curl -X PUT http://localhost:9200/test_designs_query_events -H 'content-type: application/json' -d @/tmp/data.json; do >&2 echo "Elasticsearch is unavailable - sleeping"; sleep 5; done &
