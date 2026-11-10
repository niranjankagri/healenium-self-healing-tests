#!/usr/bin/env bash
# Turns Healenium's log lines from a test run into a Markdown table: which locator broke, what it was healed to,
# and the similarity score. Used for the CI job summary.
#
#   ./mvnw verify | tee target/test-output.log
#   scripts/healing-summary.sh target/test-output.log
#
# Healenium logs two lines per healing (logger "healenium", see src/test/resources/simplelogger.properties):
#   Failed to find an element using locator By.id: email
#   Using healed locator: Scored(score=0.857..., value=By.cssSelector: input#user-email)
set -euo pipefail

log="${1:?usage: healing-summary.sh <maven output file>}"

awk '
  /healenium - Failed to find an element using locator / {
    sub(/.*using locator /, ""); broken = $0; next
  }
  /healenium - Using healed locator: / {
    line = $0
    score = line; sub(/.*score=/, "", score); sub(/,.*/, "", score)
    healed = line; sub(/.*value=/, "", healed); sub(/\)$/, "", healed)
    key = broken " -> " healed
    if (!(key in seen)) { order[++n] = key; from[key] = broken; to[key] = healed; sc[key] = score }
    seen[key]++
    next
  }
  END {
    print "## Healed locators"
    print ""
    if (n == 0) { print "No locator was healed in this run."; exit }
    print "| Locator in the page object | Healed to | Score | Times |"
    print "|---|---|---:|---:|"
    for (i = 1; i <= n; i++) {
      k = order[i]
      # Pipes inside XPath would break the table
      f = from[k]; gsub(/\|/, "\\|", f); t = to[k]; gsub(/\|/, "\\|", t)
      printf "| `%s` | `%s` | %.2f | %d |\n", f, t, sc[k], seen[k]
    }
  }
' "$log"
