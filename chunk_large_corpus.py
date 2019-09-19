# install gutenberg: (sudo) pip install gutenberg
import pip
pip.main(['install', '--user', 'gutenberg'])

from gutenberg.acquire import load_etext
from gutenberg.cleanup import strip_headers
from gutenberg._domain_model.exceptions import UnknownDownloadUriException

import os
import sys
import cPickle
from string import punctuation as punc

author2ids = None

def get_words(text_id):
    raw = strip_headers(load_etext(text_id)).strip().split()
    words = []
    for word in raw:
        if word[-1] in punc:
            words.append(word[:-1])
            words.append(word[-1])
            continue
        words.append(word)
    return words

def chunkize(words, size):
    m = max(1, size)
    n = len(words)
    chunks = [words[i:i+m] for i in xrange(0, n, m)]
    return chunks if len(chunks[-1])==size else chunks[:-1]

def generate_all(save_dir, chunk_min, chunk_max, chunk_step):
    for i in range(chunk_min, chunk_max+1, chunk_step): 
        get_chunks(i, save_dir)
            
def get_chunks(size, save_dir):
    # extraction loop
    num_digits=4
    for author in author2ids.iterkeys():
        for text_id in author2ids[author]:
            try:
                words = get_words(text_id)
                chunks = chunkize(words, size) # size: chunk size
                for idx, chunk in enumerate(chunks):
                    text = " ".join(chunk)
                    len_idx = len(str(idx))
                    pad = '0'*(num_digits-len_idx)
                    with open(os.path.join(save_dir,str(size), str(text_id)+'_'+pad+str(idx)), 'wb') as chunk_file:
                        chunk_file.write(text.encode('utf-8'))
            except UnknownDownloadUriException:
                print "Couldn't download the file " + str(text_id)
                # the following novels are missing:
                # Bronte, Charlotte: 23077
                # Hugo, Victor: 6539
                # Stoker, Bram: 6534
                # Wallace, Lew: 8810

if __name__ == '__main__':
    if len(sys.argv)!=6:
        print "FORMAT: data_dir save_dir chunk_min chunk_max chunk_step"
        sys.exit(1)

    data_dir = sys.argv[1]
    save_dir = sys.argv[2]
    chunk_min = (int)(sys.argv[3])
    chunk_max = (int)(sys.argv[4])
    chunk_step = (int)(sys.argv[5])
    
    author2ids = cPickle.load(open(os.path.join(data_dir, 'dict_author_to_ids.p'),'r'))
    generate_all(save_dir,chunk_min, chunk_max, chunk_step)
