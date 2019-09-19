# authorship-attribution-discourse
Convolutional neural network with different discourse features for authorship attribution

## Datasets
Small Gutenberg dataset (novel-9):
https://drive.google.com/open?id=0B-KvmJVuQcd9LU1DSGR4ZXJuWGc

Large Gutenberg dataset (novel-50):  
`python chunk_large_corpus.py data_dir save_dir chunk_min chunk_max chunk_step`

IMDB dataset:
https://drive.google.com/open?id=0B-KvmJVuQcd9LU1IRGhfcTJ4SGc



## Citation
Please cite our [IJCNLP 2017 paper](https://www.aclweb.org/anthology/I17-1059) as:

```
@inproceedings{ferracane-etal-2017-leveraging,
    title = "Leveraging Discourse Information Effectively for Authorship Attribution",
    author = "Ferracane, Elisa  and
      Wang, Su  and
      Mooney, Raymond",
    booktitle = "Proceedings of the Eighth International Joint Conference on Natural Language Processing (Volume 1: Long Papers)",
    month = nov,
    year = "2017",
    address = "Taipei, Taiwan",
    publisher = "Asian Federation of Natural Language Processing",
    url = "https://www.aclweb.org/anthology/I17-1059",
    pages = "584--593",
    abstract = "We explore techniques to maximize the effectiveness of discourse information in the task of authorship attribution. We present a novel method to embed discourse features in a Convolutional Neural Network text classifier, which achieves a state-of-the-art result by a significant margin. We empirically investigate several featurization methods to understand the conditions under which discourse features contribute non-trivial performance gains, and analyze discourse embeddings.",
}
```
