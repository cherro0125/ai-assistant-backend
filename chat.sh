#!/bin/sh
# Convenience wrapper around the /chat endpoint.
# Usage: ./chat.sh "your question"
#
# Note: JSON escaping/parsing here is sed-based, not a real parser. It
# handles the common case (single-line questions/answers) but won't
# correctly round-trip embedded literal newlines or \uXXXX escapes.

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 \"your question\"" >&2
  exit 1
fi

HOST="${CHAT_HOST:-http://localhost:8080}"

escaped_message=$(printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')

response=$(curl -s -w '\n%{http_code}' -X POST "$HOST/chat" \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"${escaped_message}\"}")

http_code=$(printf '%s\n' "$response" | tail -n 1)
body=$(printf '%s\n' "$response" | sed '$d')

if [ "$http_code" -lt 200 ] || [ "$http_code" -ge 300 ]; then
  echo "Error: request failed with HTTP $http_code" >&2
  echo "$body" >&2
  exit 1
fi

printf '%s\n' "$body" \
  | sed -n 's/.*"answer":"\(.*\)"}$/\1/p' \
  | sed -e 's/\\"/"/g' -e 's/\\n/\n/g' -e 's/\\\\/\\/g'
