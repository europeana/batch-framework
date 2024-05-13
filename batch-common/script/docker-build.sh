#!/bin/bash

docker build -t europeana/batch-oai-harvest $(realpath ../../batch-oai-harvest)
docker build -t europeana/batch-validation $(realpath ../../batch-validation)
docker build -t europeana/batch-transformation $(realpath ../../batch-transformation)
docker build -t europeana/batch-normalization $(realpath ../../batch-normalization)
docker build -t europeana/batch-enrichment $(realpath ../../batch-enrichment)
docker build -t europeana/batch-media $(realpath ../../batch-media)