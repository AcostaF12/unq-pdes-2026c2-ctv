#!/usr/bin/env bash
# Writes the Kubernetes CI run summary (Markdown, in Spanish) to $GITHUB_STEP_SUMMARY.
# It reads the snapshots the previous steps left in $SUMMARY_DIR and the live cluster.
# It runs with if: always(), so every piece of data is optional.
set -u

NAMESPACE=${NAMESPACE:-ctv}
DIR=${SUMMARY_DIR:-k8s-summary}
MD=$(mktemp)
NA='_No disponible (el job no llegó a este paso o falló antes)._'

# Prints a file's first line, or nothing if it is missing.
val() { [ -s "$DIR/$1" ] && head -n 1 "$DIR/$1"; }

# Formats $1 with the printf format $2, or prints a dash if $1 is empty.
# shellcheck disable=SC2059
or_dash() { if [ -n "$1" ]; then printf "$2" "$1"; else printf '—'; fi; }

# Markdown table of the pods in a `kubectl get pods -o json` file.
# $1 file, $2 epoch of the snapshot (for the age), $3 deleted pod, $4 new pod.
pods_table() {
  [ -s "$1" ] || { echo "$NA"; return; }
  echo '| Pod | Estado | Ready | Reinicios | Nodo | Edad |'
  echo '|---|---|---|---|---|---|'
  jq -r --argjson at "${2:-0}" --arg victim "${3:-}" --arg new "${4:-}" '
    def age: ($at - (.metadata.creationTimestamp | fromdateiso8601)) as $s
      | if $s < 0 then "—" elif $s < 120 then "\($s)s" else "\($s / 60 | floor)m \($s % 60)s" end;
    .items[]
    | [.status.containerStatuses // [] | .[] | select(.ready)] as $ready
    | "| `\(.metadata.name)`"
      + (if .metadata.name == $victim then " (borrado)" elif .metadata.name == $new then " (nuevo)" else "" end)
      + " | \(if .metadata.deletionTimestamp then "Terminating" else .status.phase end)"
      + " | \($ready | length)/\(.spec.containers | length)"
      + " | \([.status.containerStatuses // [] | .[].restartCount] | add // 0)"
      + " | \(.spec.nodeName // "—") | \(age) |"' "$1"
}

{
  echo '# Kubernetes CI: resumen de la corrida'
  echo
  echo "Commit \`${GITHUB_SHA:-?}\`, namespace \`$NAMESPACE\`, cluster kind \`${CLUSTER:-?}\`."
  echo

  echo '## 1. Cluster'
  echo
  version=$(kubectl version -o json 2>/dev/null)
  if [ -n "$version" ]; then
    echo "- Kubernetes (servidor): \`$(jq -r '.serverVersion.gitVersion // "?"' <<<"$version")\`"
    echo "- kubectl (cliente): \`$(jq -r '.clientVersion.gitVersion // "?"' <<<"$version")\`"
    echo
  fi
  nodes=$(kubectl get nodes -o json 2>/dev/null)
  if [ -n "$nodes" ]; then
    echo '| Nodo | Estado | Roles | Versión | IP interna | SO | Runtime |'
    echo '|---|---|---|---|---|---|---|'
    jq -r '.items[]
      | "| `\(.metadata.name)`"
        + " | \(if any(.status.conditions[]; .type == "Ready" and .status == "True") then "Ready" else "NotReady" end)"
        + " | \([.metadata.labels | keys[] | select(startswith("node-role.kubernetes.io/")) | ltrimstr("node-role.kubernetes.io/")] | join(", ") | if . == "" then "—" else . end)"
        + " | \(.status.nodeInfo.kubeletVersion)"
        + " | \([.status.addresses[] | select(.type == "InternalIP") | .address] | join(", "))"
        + " | \(.status.nodeInfo.osImage) | \(.status.nodeInfo.containerRuntimeVersion) |"' <<<"$nodes"
  else
    echo "$NA"
  fi
  echo

  echo '## 2. Objetos desplegados'
  echo
  echo '### Deployments y StatefulSets'
  echo
  workloads=$(kubectl -n "$NAMESPACE" get deployments,statefulsets -o json 2>/dev/null)
  if [ -n "$workloads" ] && [ "$(jq '.items | length' <<<"$workloads")" -gt 0 ]; then
    echo '| Tipo | Nombre | Listas/deseadas | Imagen |'
    echo '|---|---|---|---|'
    jq -r '.items[]
      | "| \(.kind) | `\(.metadata.name)` | \(.status.readyReplicas // 0)/\(.spec.replicas)"
        + " | \([.spec.template.spec.containers[].image | "`\(.)`"] | join(", ")) |"' <<<"$workloads"
  else
    echo "$NA"
  fi
  echo
  echo '### Services'
  echo
  services=$(kubectl -n "$NAMESPACE" get services -o json 2>/dev/null)
  if [ -n "$services" ] && [ "$(jq '.items | length' <<<"$services")" -gt 0 ]; then
    echo '| Nombre | Tipo | Puertos (puerto→destino) | nodePort |'
    echo '|---|---|---|---|'
    jq -r '.items[]
      | "| `\(.metadata.name)` | \(.spec.type)\(if .spec.clusterIP == "None" then " (headless)" else "" end)"
        + " | \([.spec.ports[] | "\(.port)→\(.targetPort)/\(.protocol)"] | join(", "))"
        + " | \([.spec.ports[] | .nodePort // empty | tostring] | join(", ") | if . == "" then "—" else . end) |"' <<<"$services"
  else
    echo "$NA"
  fi
  echo
  echo '### PersistentVolumeClaims'
  echo
  pvcs=$(kubectl -n "$NAMESPACE" get pvc -o json 2>/dev/null)
  if [ -n "$pvcs" ] && [ "$(jq '.items | length' <<<"$pvcs")" -gt 0 ]; then
    echo '| Nombre | Estado | Tamaño | StorageClass |'
    echo '|---|---|---|---|'
    jq -r '.items[]
      | "| `\(.metadata.name)` | \(.status.phase)"
        + " | \(.status.capacity.storage // .spec.resources.requests.storage) | \(.spec.storageClassName // "—") |"' <<<"$pvcs"
  else
    echo "$NA"
  fi
  echo

  echo '## 3. Pods al terminar el rollout'
  echo
  pods_table "$DIR/pods-rollout.json" "$(val pods-rollout.at)"
  echo

  echo '## 4. Smoke test'
  echo
  echo '| Chequeo | Esperado | Obtenido | Resultado |'
  echo '|---|---|---|---|'
  for check in 'POST /auth/login' 'GET /packages' 'GET frontend /'; do
    got=$(awk -F'\t' -v c="$check" '$1 == c { print $2 }' "$DIR/smoke.tsv" 2>/dev/null | tail -n 1)
    if [ -z "$got" ]; then result='⏭️ no se ejecutó'; got='—'
    elif [ "$got" = 200 ]; then result='✅ OK'
    else result='❌ falló'; fi
    echo "| \`$check\` | 200 | $got | $result |"
  done
  echo

  echo '## 5. Autorecuperación'
  echo
  victim=$(val victim)
  if [ -z "$victim" ]; then
    echo "$NA"
  else
    deleted_at=$(val deleted.at)
    new_pod=''; ready_after=''; created_after=''
    if [ -s "$DIR/pods-after.json" ] && [ -s "$DIR/pods-before.json" ]; then
      new_pod=$(jq -r --slurpfile before "$DIR/pods-before.json" \
        '[$before[0].items[].metadata.name] as $old
         | [.items[] | select(.metadata.deletionTimestamp == null) | select(.metadata.name as $n | $old | index($n) | not)]
         | first | .metadata.name // empty' "$DIR/pods-after.json")
    fi
    if [ -n "$new_pod" ] && [ -n "$deleted_at" ]; then
      read -r created_after ready_after < <(jq -r --arg pod "$new_pod" --argjson t0 "$deleted_at" '
        .items[] | select(.metadata.name == $pod)
        | "\((.metadata.creationTimestamp | fromdateiso8601) - $t0) \([.status.conditions[]? | select(.type == "Ready" and .status == "True") | .lastTransitionTime | fromdateiso8601 - $t0] | first // "?")"' \
        "$DIR/pods-after.json")
    fi
    login=$(val login-during.status)
    echo '| Dato | Valor |'
    echo '|---|---|'
    echo "| Pod borrado | \`$victim\` |"
    echo "| Pod nuevo que lo reemplazó | $(or_dash "$new_pod" '`%s`') |"
    echo "| Creado tras el borrado | $(or_dash "$created_after" '%s s') |"
    echo "| Ready tras el borrado | $(or_dash "$ready_after" '**%s s**') |"
    case "$login" in
      200) echo '| `POST /auth/login` durante el reemplazo | ✅ 200 (la otra réplica siguió atendiendo) |' ;;
      '') echo '| `POST /auth/login` durante el reemplazo | ⏭️ no se ejecutó |' ;;
      *) echo "| \`POST /auth/login\` durante el reemplazo | ❌ $login |" ;;
    esac
    case "$(val recovered)" in
      yes) echo '| Deployment `backend` | ✅ volvió a 2/2 réplicas listas |' ;;
      no) echo '| Deployment `backend` | ❌ no recuperó 2 réplicas listas |' ;;
      *) echo '| Deployment `backend` | — |' ;;
    esac
    echo
    echo '### Pods del backend antes del borrado'
    echo
    pods_table "$DIR/pods-before.json" "$(val pods-before.at)" "$victim"
    echo
    echo '### Pods del backend después'
    echo
    pods_table "$DIR/pods-after.json" "$(val pods-after.at)" "$victim" "$new_pod"
  fi
  echo
} >"$MD"

# Also in the log, so the text can be read or copied without opening the Summary tab.
echo '::group::Run summary (Markdown)'
cat "$MD"
echo '::endgroup::'
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then cat "$MD" >>"$GITHUB_STEP_SUMMARY"; fi
rm -f "$MD"
