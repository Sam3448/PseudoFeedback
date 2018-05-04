# PseudoFeedback
Pseudo Feedback for Elasticsearch in Information Retrieval.

## Basic structure
This work focuses on query expansion for first step, and pseudo feedback for second.

For query expansion, I experiments on both locally trained word embedding (MT bi-lingual English source) and FastText pre-trained Wiki word embedding. The results get better when using pre-trained embedding.

For retrieval part, I also indexed and searched MT and GOLD (manually) translated data. Though vocabulary matches for MT data, GOLD works better, with 3% better of miss probabillity.

Further experiments will be conducted for Pseudo Feedback.
