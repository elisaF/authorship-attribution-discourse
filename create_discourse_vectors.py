import pandas as pd
from pandas.io.common import EmptyDataError
import os
import sys

class CreateDiscourseVectors:

	def __init__(self, data_dir):
		"""
		Constructor.
		:param
			data_dir: path to source data directory.
		"""
		self.data_dir = data_dir

	def create_save(self, save_path):
		columns = ["ss", "so", "sx", "s-", "os", "oo", "ox", "o-", "xs", "xo", "xx", "x-", "-s", "-o", "-x","--"]
		files = os.listdir(self.data_dir)
		df_counts = pd.DataFrame(columns=columns, index=files)
		df_counts = df_counts.fillna(0.0) # with 0s rather than NaNs

		for file in files:
    			try:
        			df_grid = pd.read_csv(os.path.join(self.data_dir,file))
        			for count_column in df_counts:
            				first = df_counts[count_column].name[0]
            				second = df_counts[count_column].name[1]
			            	count = 0
 				        for grid_column in df_grid:
                				for i in range(0, df_grid.shape[0]-1):
                    					if df_grid.at[i,grid_column]==first and df_grid.at[i+1, grid_column]==second:
                        					count = count+1
            				df_counts.at[file,count_column]=count
        			total_transitions = df_counts.ix[file].sum()
        			df_counts.ix[file] = df_counts.ix[file]/total_transitions
    			except EmptyDataError:
        			print "Empty grid will be set to all 0s", file
		df_counts.to_csv(save_path, encoding='utf-8')

if __name__ == '__main__':
	if len(sys.argv)!=3:
		print "FORMAT: data_dir save_path"
		sys.exit(1)

	data_dir = sys.argv[1]
	save_path = sys.argv[2]

	cdv = CreateDiscourseVectors(data_dir)
	cdv.create_save(save_path)
