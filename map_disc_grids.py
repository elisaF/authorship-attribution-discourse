import pandas as pd
from pandas.io.common import EmptyDataError
import os
import sys

coarse_to_fine_mapper = {}

class MapDiscourseGrids:
    
    def __init__(self, data_dir):
        """
        Constructor.
        :param
            data_dir: path to source data directory.
        """
        self.data_dir = data_dir
        
    def build_relation_mapper(self):
        global coarse_to_fine_mapper
        coarse_to_fine_mapper['attribution.N'] = "Attribution.N"
        coarse_to_fine_mapper['attribution.S'] = "Attribution.S"

        coarse_to_fine_mapper['background.N'] = "Background.N"
        coarse_to_fine_mapper['circumstance.N'] = "Background.N"
        coarse_to_fine_mapper['background.S'] = "Background.S"
        coarse_to_fine_mapper['circumstance.S'] = "Background.S"

        coarse_to_fine_mapper['cause.N'] = "Cause.N"
        coarse_to_fine_mapper['result.N'] = "Cause.N"
        coarse_to_fine_mapper['consequence.N'] = "Cause.N"
        coarse_to_fine_mapper['cause.S'] = "Cause.S"
        coarse_to_fine_mapper['result.S'] = "Cause.S"
        coarse_to_fine_mapper['consequence.S'] = "Cause.S"

        coarse_to_fine_mapper['comparison.N'] = "Comparison.N"
        coarse_to_fine_mapper['preference.N'] = "Comparison.N"
        coarse_to_fine_mapper['analogy.N'] = "Comparison.N"
        coarse_to_fine_mapper['proportion.N'] = "Comparison.N"
        coarse_to_fine_mapper['comparison.S'] = "Comparison.S"
        coarse_to_fine_mapper['preference.S'] = "Comparison.S"
        coarse_to_fine_mapper['analogy.S'] = "Comparison.S"

        coarse_to_fine_mapper['condition.N'] = "Condition.N"
        coarse_to_fine_mapper['hypothetical.N'] = "Condition.N"
        coarse_to_fine_mapper['contingency.N'] = "Condition.N"
        coarse_to_fine_mapper['otherwise.N'] = "Condition.N"
        coarse_to_fine_mapper['condition.S'] = "Condition.S"
        coarse_to_fine_mapper['hypothetical.S'] = "Condition.S"
        coarse_to_fine_mapper['contingency.S'] = "Condition.S"
        coarse_to_fine_mapper['otherwise.S'] = "Condition.S"

        coarse_to_fine_mapper['contrast.N'] = "Contrast.N"
        coarse_to_fine_mapper['concession.N'] = "Contrast.N"
        coarse_to_fine_mapper['antithesis.N'] = "Contrast.N"
        coarse_to_fine_mapper['concession.S'] = "Contrast.S"
        coarse_to_fine_mapper['antithesis.S'] = "Contrast.S"

        coarse_to_fine_mapper['elaboration.N'] = "Elaboration.N"
        coarse_to_fine_mapper['example.N'] = "Elaboration.N"
        coarse_to_fine_mapper['definition.N'] = "Elaboration.N"
        coarse_to_fine_mapper['elaboration.S'] = "Elaboration.S"
        coarse_to_fine_mapper['example.S'] = "Elaboration.S"
        coarse_to_fine_mapper['definition.S'] = "Elaboration.S"
        
        coarse_to_fine_mapper['purpose.N'] = "Enablement.N"
        coarse_to_fine_mapper['enablement.N'] = "Enablement.N"
        coarse_to_fine_mapper['purpose.S'] = "Enablement.S"
        coarse_to_fine_mapper['enablement.S'] = "Enablement.S"

        coarse_to_fine_mapper['evaluation.N'] = "Evaluation.N"
        coarse_to_fine_mapper['interpretation.N'] = "Evaluation.N"
        coarse_to_fine_mapper['conclusion.N'] = "Evaluation.N"
        coarse_to_fine_mapper['evaluation.S'] = "Evaluation.S"
        coarse_to_fine_mapper['interpretation.S'] = "Evaluation.S"
        coarse_to_fine_mapper['conclusion.S'] = "Evaluation.S"

        coarse_to_fine_mapper['evidence.N'] = "Explanation.N"
        coarse_to_fine_mapper['explanation.N'] = "Explanation.N"
        coarse_to_fine_mapper['reason.N'] = "Explanation.N"
        coarse_to_fine_mapper['evidence.S'] = "Explanation.S"
        coarse_to_fine_mapper['explanation.S'] = "Explanation.S"
        coarse_to_fine_mapper['reason.S'] = "Explanation.S"

        coarse_to_fine_mapper['list.N'] = "Joint.N"
        coarse_to_fine_mapper['disjunction.N'] = "Joint.N"

        coarse_to_fine_mapper['manner.N'] = "Manner-Means.N"
        coarse_to_fine_mapper['means.N'] = "Manner-Means.N"
        coarse_to_fine_mapper['manner.S'] = "Manner-Means.S"
        coarse_to_fine_mapper['means.S'] = "Manner-Means.S"

        coarse_to_fine_mapper['problem.N'] = "Topic-Comment.N"
        coarse_to_fine_mapper['question.N'] = "Topic-Comment.N"
        coarse_to_fine_mapper['statement.N'] = "Topic-Comment.N"
        coarse_to_fine_mapper['topic.N'] = "Topic-Comment.N"
        coarse_to_fine_mapper['comment.N'] = "Topic-Comment.N"
        coarse_to_fine_mapper['rhetorical.N'] = "Topic-Comment.N"
        coarse_to_fine_mapper['problem.S'] = "Topic-Comment.S"
        coarse_to_fine_mapper['question.S'] = "Topic-Comment.S"
        coarse_to_fine_mapper['statement.S'] = "Topic-Comment.S"
        coarse_to_fine_mapper['topic.S'] = "Topic-Comment.S"
        coarse_to_fine_mapper['comment.S'] = "Topic-Comment.S"
        coarse_to_fine_mapper['rhetorical.S'] = "Topic-Comment.S"

        coarse_to_fine_mapper['summary.N'] = "Summary.N"
        coarse_to_fine_mapper['restatement.N'] = "Summary.N"
        coarse_to_fine_mapper['summary.S'] = "Summary.S"
        coarse_to_fine_mapper['restatement.S'] = "Summary.S"

        coarse_to_fine_mapper['temporal.N'] = "Temporal.N"
        coarse_to_fine_mapper['sequence.N'] = "Temporal.N"
        coarse_to_fine_mapper['inverted.N'] = "Temporal.N"
        coarse_to_fine_mapper['temporal.S'] = "Temporal.S"

        coarse_to_fine_mapper['same_unit.N'] = "Same-unit.N"

        coarse_to_fine_mapper['textualorganization.N'] = "Textual-organization.N"

    def map_grids(self, save_path):
        files = os.listdir(self.data_dir)
        for file in files:
            print "Mapping ", file
            try:
                df = pd.read_csv(os.path.join(self.data_dir,file), index_col=0)
                df = df.applymap(map_to_coarse)
                df.to_csv(os.path.join(save_path,file), encoding='utf-8')
            except EmptyDataError:
                print "Skipping empty grid.", file

def map_to_coarse(cell):
    coarse_rel_sets = []
    if cell == '[[]]':
        coarse_rel_sets.append("None")
    else:
        rel_lists = cell.split(']')
        for rel_list in rel_lists:
            rel_set = rel_list.split(',')
            coarse_rels = []
            for rel in rel_set:
                if rel.strip('[] ') != "":
                    coarse_rels.append(coarse_to_fine_mapper[rel.strip('[] ')])
            if coarse_rels:
                coarse_rel_sets.append(coarse_rels)
    return coarse_rel_sets

if __name__ == '__main__':
    if len(sys.argv)!=3:
        print "FORMAT: data_dir save_path"
        sys.exit(1)
    
    data_dir = sys.argv[1]
    save_path = sys.argv[2]

    mapper = MapDiscourseGrids(data_dir)
    mapper.build_relation_mapper()
    mapper.map_grids(save_path)