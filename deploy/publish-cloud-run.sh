#!/usr/bin/env bash
set -euo pipefail

service=${1:?Cloud Run service required}
smoke_path=${2:?smoke path required}
: "${CLOUDSDK_CORE_PROJECT:?}" "${CLOUDSDK_RUN_REGION:?}"
: "${GITHUB_SHA:?}" "${GITHUB_RUN_ID:?}" "${GITHUB_RUN_ATTEMPT:?}"

require_current_main() {
  [[ "$(git ls-remote origin refs/heads/main | cut -f1)" == "$GITHUB_SHA" ]] || {
    echo 'A main mudou desde o início da execução. A revisão não será promovida.' >&2
    return 1
  }
}

require_current_main
# Production infrastructure is provisioned separately. Refuse to recreate it after teardown.
gcloud run services describe "$service" --format='value(status.url)' >/dev/null

registry="${CLOUDSDK_RUN_REGION}-docker.pkg.dev"
image="${registry}/${CLOUDSDK_CORE_PROJECT}/eyes-release/${service}"
suffix="g${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
revision="${service}-${suffix}"
tagged_image="${image}:${GITHUB_SHA}-${suffix}"
gcloud auth configure-docker "$registry" --quiet
docker tag eyes-release "$tagged_image"
docker push "$tagged_image"
digest=$(docker buildx imagetools inspect "$tagged_image" --format '{{json .Manifest}}' | jq -er .digest)
[[ "$digest" =~ ^sha256:[a-f0-9]{64}$ ]]

cleanup() {
  gcloud run services update-traffic "$service" --remove-tags="$suffix" --quiet >/dev/null || true
}
trap cleanup EXIT
gcloud run deploy "$service" --image="${image}@${digest}" --revision-suffix="$suffix" \
  --update-labels="source-sha=$GITHUB_SHA" --no-traffic --tag="$suffix" --quiet
candidate=$(gcloud run services describe "$service" --format=json |
  jq -er --arg tag "$suffix" '.status.traffic[] | select(.tag == $tag) | .url')
body=$(curl --fail --silent --show-error --proto '=https' --max-time 90 \
  --retry 4 --retry-all-errors --retry-max-time 150 "${candidate}${smoke_path}")
if [[ "$smoke_path" == /actuator/health ]]; then
  jq -e '.status == "UP"' <<<"$body" >/dev/null
else
  grep -q '<app-root' <<<"$body"
fi
require_current_main
gcloud run services update-traffic "$service" --to-revisions="${revision}=100" --quiet
printf 'Cloud Run %s\n\n- Commit `%s`\n- Imagem `%s@%s`\n- Revisão `%s`\n- Smoke HTTPS aprovado\n' \
  "$service" "$GITHUB_SHA" "$image" "$digest" "$revision" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
