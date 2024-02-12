#!/bin/bash
cd batch-oai-harvest && docker build . -t europeana/batch-oai-harvest && cd ..

cd batch-validation && docker build . -t europeana/batch-validation && cd ..

cd batch-transformation && docker build . -t europeana/batch-transformation && cd ..

cd batch-normalization && docker build . -t europeana/batch-normalization && cd ..

cd batch-enrichment && docker build . -t europeana/batch-enrichment && cd ..

cd batch-media && docker build . -t europeana/batch-media && cd ..