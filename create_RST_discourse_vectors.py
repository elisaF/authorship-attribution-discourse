from __future__ import division
import pandas as pd
from pandas.io.common import EmptyDataError
import os
import sys
from collections import Counter

class CreateDiscourseVectors:

    def __init__(self, data_dir):
        """
        Constructor.
        :param
            data_dir: path to source data directory.
        """
        self.data_dir = data_dir
        
    def get_counts(self, file):
        df = pd.read_csv(os.path.join(self.data_dir,file), index_col=0)
        flat_list = self.flatten_dataframe(df)
        rel_counter = Counter(flat_list)
        coarse_rel_counter = self.get_coarse_grain_rels(rel_counter)
        #sanity check: make sure every relation gets mapped to a coarse-grained one
        assert (sum(rel_counter.values()) == sum(coarse_rel_counter.values())), "Counters are not equal, so not every relation was mapped. rel_counter: %r, coarse relations: %r" % (rel_counter, coarse_rel_counter)
        return coarse_rel_counter
        
    def flatten_dataframe(self, df):
        flat_list = []
        my_list = df.values.tolist()
        for rel_sets in my_list:
            for rel_set in rel_sets:
                rel_list = rel_set.split(',')
                for rel in rel_list:
                    if rel == '[[]]':
                        flat_list.append("None")
                    else:
                        flat_list.append(rel.strip('[] '))
        return flat_list
        
    def get_coarse_grain_rels(self, rel_counter):
        coarse_grain_rel_counter = Counter()
        coarse_grain_rel_counter.update({'Attribution.N':rel_counter['attribution.N']})
        coarse_grain_rel_counter.update({'Attribution.S':rel_counter['attribution.S']})

        coarse_grain_rel_counter.update({'Background.N':rel_counter['background.N']})
        coarse_grain_rel_counter.update({'Background.N':rel_counter['circumstance.N']})
        coarse_grain_rel_counter.update({'Background.S':rel_counter['background.S']})
        coarse_grain_rel_counter.update({'Background.S':rel_counter['circumstance.S']})

        coarse_grain_rel_counter.update({'Cause.N':rel_counter['cause.N']})
        coarse_grain_rel_counter.update({'Cause.N':rel_counter['result.N']})
        coarse_grain_rel_counter.update({'Cause.N':rel_counter['consequence.N']})
        coarse_grain_rel_counter.update({'Cause.S':rel_counter['cause.S']})
        coarse_grain_rel_counter.update({'Cause.S':rel_counter['result.S']})
        coarse_grain_rel_counter.update({'Cause.S':rel_counter['consequence.S']})

        coarse_grain_rel_counter.update({'Comparison.N':rel_counter['comparison.N']})
        coarse_grain_rel_counter.update({'Comparison.N':rel_counter['preference.N']})
        coarse_grain_rel_counter.update({'Comparison.N':rel_counter['analogy.N']})
        coarse_grain_rel_counter.update({'Comparison.N':rel_counter['proportion.N']})
        coarse_grain_rel_counter.update({'Comparison.S':rel_counter['comparison.S']})
        coarse_grain_rel_counter.update({'Comparison.S':rel_counter['preference.S']})
        coarse_grain_rel_counter.update({'Comparison.S':rel_counter['analogy.S']})

        coarse_grain_rel_counter.update({'Condition.N':rel_counter['condition.N']})
        coarse_grain_rel_counter.update({'Condition.N':rel_counter['hypothetical.N']})
        coarse_grain_rel_counter.update({'Condition.N':rel_counter['contingency.N']})
        coarse_grain_rel_counter.update({'Condition.N':rel_counter['otherwise.N']})
        coarse_grain_rel_counter.update({'Condition.S':rel_counter['condition.S']})
        coarse_grain_rel_counter.update({'Condition.S':rel_counter['hypothetical.S']})
        coarse_grain_rel_counter.update({'Condition.S':rel_counter['contingency.S']})
        coarse_grain_rel_counter.update({'Condition.S':rel_counter['otherwise.S']})

        coarse_grain_rel_counter.update({'Contrast.N':rel_counter['contrast.N']})
        coarse_grain_rel_counter.update({'Contrast.N':rel_counter['concession.N']})
        coarse_grain_rel_counter.update({'Contrast.N':rel_counter['antithesis.N']})
        coarse_grain_rel_counter.update({'Contrast.S':rel_counter['concession.S']})
        coarse_grain_rel_counter.update({'Contrast.S':rel_counter['antithesis.S']})

        coarse_grain_rel_counter.update({'Elaboration.N':rel_counter['elaboration.N']})
        coarse_grain_rel_counter.update({'Elaboration.N':rel_counter['example.N']})
        coarse_grain_rel_counter.update({'Elaboration.N':rel_counter['definition.N']})
        coarse_grain_rel_counter.update({'Elaboration.S':rel_counter['elaboration.S']})
        coarse_grain_rel_counter.update({'Elaboration.S':rel_counter['example.S']})
        coarse_grain_rel_counter.update({'Elaboration.S':rel_counter['definition.S']})

        coarse_grain_rel_counter.update({'Enablement.N':rel_counter['purpose.N']})
        coarse_grain_rel_counter.update({'Enablement.N':rel_counter['enablement.N']})
        coarse_grain_rel_counter.update({'Enablement.S':rel_counter['purpose.S']})
        coarse_grain_rel_counter.update({'Enablement.S':rel_counter['enablement.S']})

        coarse_grain_rel_counter.update({'Evaluation.N':rel_counter['evaluation.N']})
        coarse_grain_rel_counter.update({'Evaluation.N':rel_counter['interpretation.N']})
        coarse_grain_rel_counter.update({'Evaluation.N':rel_counter['conclusion.N']})
        coarse_grain_rel_counter.update({'Evaluation.S':rel_counter['evaluation.S']})
        coarse_grain_rel_counter.update({'Evaluation.S':rel_counter['interpretation.S']})
        coarse_grain_rel_counter.update({'Evaluation.S':rel_counter['conclusion.S']})

        coarse_grain_rel_counter.update({'Explanation.N':rel_counter['evidence.N']})
        coarse_grain_rel_counter.update({'Explanation.N':rel_counter['explanation.N']})
        coarse_grain_rel_counter.update({'Explanation.N':rel_counter['reason.N']})
        coarse_grain_rel_counter.update({'Explanation.S':rel_counter['evidence.S']})
        coarse_grain_rel_counter.update({'Explanation.S':rel_counter['explanation.S']})
        coarse_grain_rel_counter.update({'Explanation.S':rel_counter['reason.S']})

        coarse_grain_rel_counter.update({'Joint.N':rel_counter['list.N']})
        coarse_grain_rel_counter.update({'Joint.N':rel_counter['disjunction.N']})

        coarse_grain_rel_counter.update({'Manner-Means.N':rel_counter['manner.N']})
        coarse_grain_rel_counter.update({'Manner-Means.N':rel_counter['means.N']})
        coarse_grain_rel_counter.update({'Manner-Means.S':rel_counter['manner.S']})
        coarse_grain_rel_counter.update({'Manner-Means.S':rel_counter['means.S']})

        coarse_grain_rel_counter.update({'Topic-Comment.N':rel_counter['problem.N']})
        coarse_grain_rel_counter.update({'Topic-Comment.N':rel_counter['question.N']})
        coarse_grain_rel_counter.update({'Topic-Comment.N':rel_counter['statement.N']})
        coarse_grain_rel_counter.update({'Topic-Comment.N':rel_counter['topic.N']})
        coarse_grain_rel_counter.update({'Topic-Comment.N':rel_counter['comment.N']})
        coarse_grain_rel_counter.update({'Topic-Comment.N':rel_counter['rhetorical.N']})
        coarse_grain_rel_counter.update({'Topic-Comment.S':rel_counter['problem.S']})
        coarse_grain_rel_counter.update({'Topic-Comment.S':rel_counter['question.S']})
        coarse_grain_rel_counter.update({'Topic-Comment.S':rel_counter['statement.S']})
        coarse_grain_rel_counter.update({'Topic-Comment.S':rel_counter['topic.S']})
        coarse_grain_rel_counter.update({'Topic-Comment.S':rel_counter['comment.S']})
        coarse_grain_rel_counter.update({'Topic-Comment.S':rel_counter['rhetorical.S']})

        coarse_grain_rel_counter.update({'Summary.N':rel_counter['summary.N']})
        coarse_grain_rel_counter.update({'Summary.N':rel_counter['restatement.N']})
        coarse_grain_rel_counter.update({'Summary.S':rel_counter['summary.S']})
        coarse_grain_rel_counter.update({'Summary.S':rel_counter['restatement.S']})

        coarse_grain_rel_counter.update({'Temporal.N':rel_counter['temporal.N']})
        coarse_grain_rel_counter.update({'Temporal.N':rel_counter['sequence.N']})
        coarse_grain_rel_counter.update({'Temporal.N':rel_counter['inverted.N']})
        coarse_grain_rel_counter.update({'Temporal.S':rel_counter['temporal.S']})

        coarse_grain_rel_counter.update({'Same-unit.N':rel_counter['same_unit.N']})

        coarse_grain_rel_counter.update({'Textual-organization.N':rel_counter['textualorganization.N']})

        coarse_grain_rel_counter.update({'None':rel_counter['None']})

        return coarse_grain_rel_counter
        
    def create_save(self, save_path):
        columns = ['Attribution.N', 'Attribution.S', 'Background.N', 'Background.S', 'Cause.N', 'Cause.S', 'Comparison.N', 'Comparison.S', 
                   'Condition.N', 'Condition.S', 'Contrast.N', 'Contrast.S', 'Elaboration.N', 'Elaboration.S', 'Enablement.N', 'Enablement.S',
                   'Evaluation.N', 'Evaluation.S', 'Explanation.N', 'Explanation.S', 'Joint.N', 'Manner-Means.N', 'Manner-Means.S', 
                   'Topic-Comment.N','Topic-Comment.S', 'Summary.N', 'Summary.S', 'Temporal.N', 'Temporal.S', 'Same-unit.N', 
                   'Textual-organization.N', 'None']
        files = os.listdir(self.data_dir)
        df_counts = pd.DataFrame(columns=columns, index=files)
        df_counts = df_counts.fillna(0.0) # with 0s rather than NaNs

        for file in files:
            try:
                coarse_rel_counter = self.get_counts(file)
                total_counts = sum(coarse_rel_counter.values())
                for count_column in df_counts:
                    if total_counts == 0:
                        prob = 0
                    else:
                        prob = coarse_rel_counter[count_column]/total_counts
                    df_counts.at[file,count_column]=prob
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
        