#!/bin/bash

# Requires to have already been logged in to the registry we are deploying the images
# ${1} docker_registry_and_prefix e.g. "default" or empty(for docker hub or local), "example.com/prefix/"(for an example repository with prefix)
function main() {
  docker_registry_and_prefix="$1"
  echo "Docker registry and prefix \"$1\""
  docker_build_and_tag
  docker_push
}

function docker_build_and_tag() {
  docker build -t "${docker_registry_and_prefix}europeana/batch-oai-harvest" "$(realpath ../../batch-oai-harvest)"
  docker build -t "${docker_registry_and_prefix}europeana/batch-validation" "$(realpath ../../batch-validation)"
  docker build -t "${docker_registry_and_prefix}europeana/batch-transformation" "$(realpath ../../batch-transformation)"
  docker build -t "${docker_registry_and_prefix}europeana/batch-normalization" "$(realpath ../../batch-normalization)"
  docker build -t "${docker_registry_and_prefix}europeana/batch-enrichment" "$(realpath ../../batch-enrichment)"
  docker build -t "${docker_registry_and_prefix}europeana/batch-media" "$(realpath ../../batch-media)"
  docker build -t "${docker_registry_and_prefix}europeana/batch-indexing" "$(realpath ../../batch-indexing)"
}


function docker_push() {
  docker push "${docker_registry_and_prefix}europeana/batch-oai-harvest"
  docker push "${docker_registry_and_prefix}europeana/batch-validation"
  docker push "${docker_registry_and_prefix}europeana/batch-transformation"
  docker push "${docker_registry_and_prefix}europeana/batch-normalization"
  docker push "${docker_registry_and_prefix}europeana/batch-enrichment"
  docker push "${docker_registry_and_prefix}europeana/batch-media"
  docker push "${docker_registry_and_prefix}europeana/batch-indexing"
}

main "$@"
