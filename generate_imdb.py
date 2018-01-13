import pandas as pd
import numpy as np
import os
import sys

def generate_imdb(data_dir, save_dir):
    num_digits = 4
    df = pd.read_csv(os.path.join(data_dir, "imdb62_clean.txt"), sep='\t', header=None, names=["author_id", "text"])
    
    for author in np.unique(df.author_id):
        for idx, text in enumerate(df[df.author_id==author].text):
            len_idx = len(str(idx))
            pad = '0'*(num_digits-len_idx)
            work_id = str(author) + "_" + pad + str(idx)
            with open(os.path.join(save_dir, work_id), 'wb') as review_file:
                review_file.write(text)
            
if __name__ == '__main__':
	if len(sys.argv)!=3:
		print "FORMAT: data_dir save_dir"
		sys.exit(1)

	data_dir = sys.argv[1]
	save_dir = sys.argv[2]

	gd = generate_imdb(data_dir, save_dir)